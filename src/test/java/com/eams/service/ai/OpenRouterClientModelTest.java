package com.eams.service.ai;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** How the AI_MODEL setting is turned into the request body. */
class OpenRouterClientModelTest {

    private Map<String, Object> bodyFor(String setting) {
        Map<String, Object> body = new LinkedHashMap<>();
        OpenRouterClient.putModel(body, setting);
        return body;
    }

    @Test
    void singleModelIsSentAsModel() {
        assertThat(bodyFor(" a/one:free ")).containsExactly(Map.entry("model", "a/one:free"));
    }

    @Test
    void commaSeparatedModelsBecomeAFallbackList() {
        assertThat(bodyFor("a/one:free, b/two:free,,c/three:free"))
                .containsExactly(Map.entry("models", List.of("a/one:free", "b/two:free", "c/three:free")));
    }

    @Test
    void fallbackListIsCappedAtOpenRouterLimit() {
        assertThat(bodyFor("a,b,c,d")).containsEntry("models", List.of("a", "b", "c"));
    }
}
