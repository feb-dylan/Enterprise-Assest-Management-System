package com.eams.service.ai;

import com.eams.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal client for the OpenRouter Chat Completions API
 * (POST {baseUrl}/chat/completions, OpenAI-compatible format).
 *
 * Messages and tools are plain maps so they match the documented JSON exactly:
 *   {"role":"user","content":"..."}
 *   {"role":"user","content":[{"type":"text","text":"..."},
 *                             {"type":"image_url","image_url":{"url":"data:image/png;base64,..."}}]}
 *   {"role":"tool","tool_call_id":"call_1","content":"..."}
 *
 * AI is off (isEnabled() == false) until OPENROUTER_API_KEY and AI_MODEL are set.
 */
@Component
public class OpenRouterClient {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterClient.class);

    /** One tool call requested by the model. {@code arguments} is a JSON string. */
    public record ToolCall(String id, String name, String arguments) {}

    /**
     * @param content          the model's text (may be null when it only calls tools)
     * @param toolCalls        tools the model wants to run (empty when it answered)
     * @param finishReason     "stop", "tool_calls", "length", "content_filter", "error"
     * @param assistantMessage the raw assistant message, to append to the conversation unchanged
     */
    public record ChatResult(String content, List<ToolCall> toolCalls, String finishReason,
                             Map<String, Object> assistantMessage) {}

    /** The API answered with an error (bad key, unknown model, rate limit, ...). */
    public static class AiException extends RuntimeException {
        private final int status;

        public AiException(int status, String message) {
            super(message);
            this.status = status;
        }

        public int status() {
            return status;
        }
    }

    private final AppProperties properties;
    private final JsonMapper json = JsonMapper.builder().build();
    private volatile RestClient restClient;

    public OpenRouterClient(AppProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.ai() != null && properties.ai().isConfigured();
    }

    public String chatModel() {
        return properties.ai().model();
    }

    public String visionModel() {
        return properties.ai().effectiveVisionModel();
    }

    /**
     * Sends one chat request.
     *
     * @param tools tool definitions, or null/empty for none
     */
    @SuppressWarnings("unchecked")
    public ChatResult chat(String model, List<Map<String, Object>> messages,
                           List<Map<String, Object>> tools, int maxTokens) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", maxTokens);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }

        String responseText = client().post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.writeValueAsString(body))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    String errorBody = new String(response.getBody().readAllBytes());
                    throw new AiException(response.getStatusCode().value(), errorMessage(errorBody));
                })
                .body(String.class);

        Map<String, Object> response = json.readValue(responseText, Map.class);

        // OpenRouter can also report errors inside a 200 response
        if (response.get("error") instanceof Map<?, ?> error) {
            Object code = error.get("code");
            throw new AiException(code instanceof Number n ? n.intValue() : 502,
                    String.valueOf(error.get("message")));
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new AiException(502, "The AI provider returned no answer");
        }
        Map<String, Object> choice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) choice.getOrDefault("message", Map.of());

        List<ToolCall> toolCalls = new ArrayList<>();
        if (message.get("tool_calls") instanceof List<?> calls) {
            for (Object c : calls) {
                Map<String, Object> call = (Map<String, Object>) c;
                Map<String, Object> function = (Map<String, Object>) call.getOrDefault("function", Map.of());
                toolCalls.add(new ToolCall(
                        String.valueOf(call.get("id")),
                        String.valueOf(function.get("name")),
                        function.get("arguments") == null ? "{}" : String.valueOf(function.get("arguments"))));
            }
        }

        // Keep only the fields the API accepts back in the conversation.
        Map<String, Object> assistant = new LinkedHashMap<>();
        assistant.put("role", "assistant");
        assistant.put("content", message.get("content"));
        if (message.get("tool_calls") != null) {
            assistant.put("tool_calls", message.get("tool_calls"));
        }

        Object content = message.get("content");
        return new ChatResult(
                content == null ? null : String.valueOf(content),
                toolCalls,
                choice.get("finish_reason") == null ? null : String.valueOf(choice.get("finish_reason")),
                assistant);
    }

    // ---------------------------------------------------------

    private RestClient client() {
        if (restClient == null) {
            synchronized (this) {
                if (restClient == null) {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(Duration.ofSeconds(10));
                    factory.setReadTimeout(Duration.ofSeconds(120));

                    String baseUrl = properties.ai().baseUrl() == null || properties.ai().baseUrl().isBlank()
                            ? "https://openrouter.ai/api/v1"
                            : properties.ai().baseUrl();

                    restClient = RestClient.builder()
                            .baseUrl(baseUrl)
                            .requestFactory(factory)
                            .defaultHeader("Authorization", "Bearer " + properties.ai().apiKey())
                            // Optional attribution headers recommended by OpenRouter
                            .defaultHeader("HTTP-Referer", properties.frontendUrl())
                            .defaultHeader("X-Title", "EAMS")
                            .build();
                }
            }
        }
        return restClient;
    }

    @SuppressWarnings("unchecked")
    private String errorMessage(String body) {
        try {
            Map<String, Object> parsed = json.readValue(body, Map.class);
            if (parsed.get("error") instanceof Map<?, ?> error && error.get("message") != null) {
                return String.valueOf(error.get("message"));
            }
        } catch (Exception ignored) {
            // not JSON
        }
        log.debug("AI provider error body: {}", body);
        return body == null || body.isBlank() ? "AI provider error" : body.substring(0, Math.min(300, body.length()));
    }
}
