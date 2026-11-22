package com.eams.controller;

import com.eams.dto.request.DamageCreateRequest;
import com.eams.dto.request.DamageUpdateRequest;
import com.eams.dto.response.DamageResponse;
import com.eams.service.DamageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/damage")
@RequiredArgsConstructor
public class DamageController {

    private final DamageService damageService;

    /** Reporter = logged-in employee (employeeId in the body is ignored). */
    @PostMapping
    public ResponseEntity<DamageResponse> createDamageReport(
            @Valid @RequestBody DamageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(damageService.createDamageReport(request));
    }

    /**
     * Upload up to 5 photos (form field "files", or "file" for one).
     * Starts AI triage in the background when OPENROUTER_API_KEY and AI_MODEL are set.
     */
    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DamageResponse> addPhotos(
            @PathVariable Long id,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        List<MultipartFile> all = new java.util.ArrayList<>();
        if (files != null) {
            all.addAll(files);
        }
        if (file != null) {
            all.add(file);
        }
        return ResponseEntity.ok(damageService.addPhotos(id, all));
    }

    @PostMapping("/{id}/ai-triage")
    public ResponseEntity<DamageResponse> runAiTriage(@PathVariable Long id) {
        return ResponseEntity.accepted().body(damageService.requestAiTriage(id));
    }

    @GetMapping({"/my", "/my/{employeeId}"})
    public ResponseEntity<List<DamageResponse>> getMyDamageReports(
            @PathVariable(required = false) Long employeeId) {
        return ResponseEntity.ok(damageService.getMyDamageReports(employeeId));
    }

    @GetMapping
    public ResponseEntity<List<DamageResponse>> getAllDamageReports() {
        return ResponseEntity.ok(damageService.getAllDamageReports());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DamageResponse> getDamageReport(@PathVariable Long id) {
        return ResponseEntity.ok(damageService.getDamageReportById(id));
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<List<DamageResponse>> getDamageReportsByAsset(@PathVariable Long assetId) {
        return ResponseEntity.ok(damageService.getDamageReportsByAsset(assetId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DamageResponse> updateDamageReport(
            @PathVariable Long id, @Valid @RequestBody DamageUpdateRequest request) {
        return ResponseEntity.ok(damageService.updateDamageReport(id, request));
    }
}
