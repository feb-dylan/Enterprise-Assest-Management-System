package com.eams.controller;

import com.eams.dto.request.Requests.AssistantRequest;
import com.eams.dto.response.Responses.AssistantResponse;
import com.eams.service.ai.AiTriageService;
import com.eams.service.ai.AssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;
    private final AiTriageService aiTriageService;

    /** Lets the frontend hide AI buttons when no API key is configured. */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        return ResponseEntity.ok(Map.of(
                "assistantEnabled", assistantService.isEnabled(),
                "damageTriageEnabled", aiTriageService.isEnabled()));
    }

    /** "Ask EAMS": natural-language question about the asset data (admin / manager). */
    @PostMapping("/ask")
    public ResponseEntity<AssistantResponse> ask(@Valid @RequestBody AssistantRequest request) {
        return ResponseEntity.ok(assistantService.ask(request.question()));
    }
}
