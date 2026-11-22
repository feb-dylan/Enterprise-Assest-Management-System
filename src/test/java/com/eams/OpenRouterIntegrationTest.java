package com.eams;

import com.eams.entity.User;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real AI code against a FAKE OpenRouter server, so the request format,
 * the tool-calling loop and the answer parsing are tested without a key or quota.
 */
class OpenRouterIntegrationTest extends IntegrationTestBase {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};

    private static final List<String> requests = new CopyOnWriteArrayList<>();
    private static final List<String> authHeaders = new CopyOnWriteArrayList<>();
    private static final HttpServer fakeOpenRouter = startFakeServer();

    @DynamicPropertySource
    static void aiProperties(DynamicPropertyRegistry registry) {
        registry.add("app.ai.enabled", () -> "true");
        registry.add("app.ai.api-key", () -> "test-key");
        registry.add("app.ai.model", () -> "test/chat-model:free");
        registry.add("app.ai.vision-model", () -> "test/vision-model:free");
        registry.add("app.ai.base-url",
                () -> "http://localhost:" + fakeOpenRouter.getAddress().getPort() + "/api/v1");
    }

    @AfterAll
    static void stop() {
        fakeOpenRouter.stop(0);
    }

    @Test
    void askEamsRunsToolsAndReturnsTheAnswer() throws Exception {
        String admin = login(createUser("ADMIN"));

        doPost(admin, "/api/assistant/ask", "{\"question\":\"How many assets per status?\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer", containsString("Answer based on tools")))
                .andExpect(jsonPath("$.toolsUsed", hasItem("count_assets")))
                .andExpect(jsonPath("$.aiEnabled").value(true));

        String first = requests.stream().filter(r -> r.contains("How many assets per status"))
                .findFirst().orElseThrow();
        assertThat(first).contains("\"model\":\"test/chat-model:free\"", "\"tools\"", "count_assets");
        assertThat(requests.stream().anyMatch(r -> r.contains("\"role\":\"tool\"")
                && r.contains("\"tool_call_id\":\"call_1\""))).isTrue();
        assertThat(authHeaders).contains("Bearer test-key");
    }

    @Test
    void rateLimitBecomesFriendly503() throws Exception {
        String admin = login(createUser("ADMIN"));
        doPost(admin, "/api/assistant/ask", "{\"question\":\"QUOTA please\"}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message", containsString("free AI quota")));
    }

    @Test
    void damagePhotoIsTriagedByTheVisionModel() throws Exception {
        String admin = login(createUser("ADMIN"));
        String manager = login(createUser("MANAGER"));
        User empUser = createUser("EMPLOYEE");
        createEmployeeProfile(empUser);
        String emp = login(empUser);

        long assetId = createAsset(admin, createCategory(admin), 900);
        long req = id(doPost(emp, "/api/requests", "{\"assetId\":" + assetId + ",\"reason\":\"work\"}"));
        doPut(manager, "/api/requests/" + req + "/approve", null);
        doPut(admin, "/api/requests/" + req + "/assign", null);
        long damageId = id(doPost(emp, "/api/damage",
                "{\"assetId\":" + assetId + ",\"description\":\"Screen cracked after a drop\"}"));

        mvc.perform(multipart("/api/damage/" + damageId + "/photos")
                        .file(new MockMultipartFile("files", "crack.png", "image/png", PNG))
                        .header("Authorization", "Bearer " + emp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiStatus").value("PENDING"));

        // triage runs in the background - wait for it
        String aiStatus = null;
        for (int i = 0; i < 50 && !"COMPLETED".equals(aiStatus); i++) {
            Thread.sleep(100);
            aiStatus = json(doGet(admin, "/api/damage/" + damageId), "$.aiStatus");
        }

        doGet(admin, "/api/damage/" + damageId)
                .andExpect(jsonPath("$.aiStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.aiSeverity").value("HIGH"))
                .andExpect(jsonPath("$.aiDamageType").value("CRACKED_SCREEN"))
                .andExpect(jsonPath("$.aiSuggestedAction").value("REPAIR"))
                .andExpect(jsonPath("$.aiCostMin").value(80.0))
                .andExpect(jsonPath("$.aiCostMax").value(150.0));

        String visionRequest = requests.stream().filter(r -> r.contains("image_url")).findFirst().orElseThrow();
        assertThat(visionRequest)
                .contains("\"model\":\"test/vision-model:free\"")
                .contains("data:image/png;base64,");
    }

    // ---------------------------------------------------------
    // Fake OpenRouter: /api/v1/chat/completions
    // ---------------------------------------------------------

    private static HttpServer startFakeServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api/v1/chat/completions", exchange -> {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                requests.add(body);
                authHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));

                int code = 200;
                String response;
                if (body.contains("QUOTA please")) {
                    code = 429;
                    response = "{\"error\":{\"code\":429,\"message\":\"Rate limit exceeded: free-models-per-day\"}}";
                } else if (body.contains("image_url")) {
                    // Wrapped in a markdown fence on purpose: the parser must cope with it
                    String content = "```json\\n{\\\"damageType\\\":\\\"cracked screen\\\",\\\"severity\\\":\\\"high\\\","
                            + "\\\"suggestedAction\\\":\\\"REPAIR\\\",\\\"summary\\\":\\\"Diagonal crack across the display.\\\","
                            + "\\\"estimatedCostMin\\\":150,\\\"estimatedCostMax\\\":80,\\\"safetyRisk\\\":false}\\n```";
                    response = message("{\"role\":\"assistant\",\"content\":\"" + content + "\"}", "stop");
                } else if (body.contains("\"role\":\"tool\"")) {
                    response = message("{\"role\":\"assistant\",\"content\":\"Answer based on tools: "
                            + "most assets are AVAILABLE.\"}", "stop");
                } else {
                    response = message("{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"id\":\"call_1\","
                            + "\"type\":\"function\",\"function\":{\"name\":\"count_assets\","
                            + "\"arguments\":\"{\\\"group_by\\\":\\\"status\\\"}\"}}]}", "tool_calls");
                }

                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(code, bytes.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String message(String message, String finishReason) {
        return "{\"id\":\"gen-1\",\"choices\":[{\"index\":0,\"finish_reason\":\"" + finishReason
                + "\",\"message\":" + message + "}]}";
    }
}
