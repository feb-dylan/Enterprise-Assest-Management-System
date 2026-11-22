package com.eams.controller;

import com.eams.dto.request.Requests.AuditCampaignRequest;
import com.eams.dto.request.Requests.AuditScanRequest;
import com.eams.dto.response.Responses.AuditCampaignResponse;
import com.eams.dto.response.Responses.AuditLogEntry;
import com.eams.dto.response.Responses.AuditReconciliation;
import com.eams.dto.response.Responses.AuditScanResponse;
import com.eams.repository.AuditEventRepository;
import com.eams.service.AuditCampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Physical stock-take campaigns (/api/audits) and the system audit log (/api/audit-log).
 */
@RestController
@RequiredArgsConstructor
public class AuditController {

    private final AuditCampaignService campaignService;
    private final AuditEventRepository auditEventRepository;

    @PostMapping("/api/audits")
    public ResponseEntity<AuditCampaignResponse> create(@Valid @RequestBody AuditCampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.create(request));
    }

    @GetMapping("/api/audits")
    public ResponseEntity<List<AuditCampaignResponse>> list() {
        return ResponseEntity.ok(campaignService.list());
    }

    /** Scan one QR code during a stock-take. */
    @PostMapping("/api/audits/{id}/scans")
    public ResponseEntity<AuditScanResponse> scan(
            @PathVariable Long id, @Valid @RequestBody AuditScanRequest request) {
        return ResponseEntity.ok(campaignService.scan(id, request));
    }

    /** Found / misplaced / damaged / MISSING assets for this campaign. */
    @GetMapping("/api/audits/{id}")
    public ResponseEntity<AuditReconciliation> reconciliation(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.reconciliation(id));
    }

    @PostMapping("/api/audits/{id}/close")
    public ResponseEntity<AuditReconciliation> close(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.close(id));
    }

    /** Latest system audit entries (who did what, when). */
    @GetMapping("/api/audit-log")
    public ResponseEntity<List<AuditLogEntry>> auditLog(@RequestParam(defaultValue = "100") int limit) {
        int size = Math.min(Math.max(limit, 1), 500);
        return ResponseEntity.ok(auditEventRepository.findAllByOrderByIdDesc(PageRequest.of(0, size))
                .stream()
                .map(e -> new AuditLogEntry(e.getId(), e.getAssetId(), e.getEntityType(), e.getEntityId(),
                        e.getAction(), e.getDetails(), e.getActorEmail(), e.getCreatedAt()))
                .toList());
    }
}
