package com.eams.controller;

import com.eams.dto.request.Requests.IdleAssetReminderRequest;
import com.eams.dto.response.AssetRequestResponse;
import com.eams.dto.response.Responses.AssetHealthResponse;
import com.eams.dto.response.Responses.IdleAsset;
import com.eams.dto.response.Responses.InsightsSummary;
import com.eams.service.AssetHealthService;
import com.eams.service.InsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Management insights (admin + manager). */
@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightsController {

    private final InsightsService insightsService;
    private final AssetHealthService healthService;

    /** KPI cards: book value, repair spend, money at risk, idle assets, purchases avoided, charts. */
    @GetMapping("/summary")
    public ResponseEntity<InsightsSummary> summary() {
        return ResponseEntity.ok(insightsService.summary());
    }

    /** "Top N assets to replace" with reasons. */
    @GetMapping("/replacement-candidates")
    public ResponseEntity<List<AssetHealthResponse>> replacementCandidates(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(insightsService.replacementCandidates(limit));
    }

    /** Health score of every active asset, worst first. */
    @GetMapping("/asset-health")
    public ResponseEntity<List<AssetHealthResponse>> assetHealth() {
        return ResponseEntity.ok(healthService.all());
    }

    /** Assets sitting unused (Green IT: redeploy before buying new). */
    @GetMapping("/idle-assets")
    public ResponseEntity<List<IdleAsset>> idleAssets(@RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(insightsService.idleAssets(days));
    }

    /** The waiting "any item of this category" requests an idle asset could fill. */
    @GetMapping("/idle-assets/{assetId}/waiting-requests")
    public ResponseEntity<List<AssetRequestResponse>> waitingRequests(@PathVariable Long assetId) {
        return ResponseEntity.ok(insightsService.waitingRequestsFor(assetId));
    }

    /** Admin only: ask the managers to approve a pending request this idle asset could fill. */
    @PostMapping("/idle-assets/{assetId}/remind")
    public ResponseEntity<Void> remindManagers(@PathVariable Long assetId,
                                             @Valid @RequestBody(required = false) IdleAssetReminderRequest body) {
        insightsService.remindManagers(assetId, body != null ? body.note() : null);
        return ResponseEntity.noContent().build();
    }
}
