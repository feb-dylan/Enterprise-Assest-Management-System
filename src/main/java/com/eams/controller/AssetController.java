package com.eams.controller;

import com.eams.dto.request.AssetRequestDto;
import com.eams.dto.request.Requests.RetireAssetRequest;
import com.eams.dto.response.AssetResponseDto;
import com.eams.dto.response.Responses.AssetHealthResponse;
import com.eams.dto.response.Responses.AssetScanResponse;
import com.eams.dto.response.Responses.TimelineEvent;
import com.eams.entity.AssetStatus;
import com.eams.service.AssetHealthService;
import com.eams.service.AssetService;
import com.eams.service.QrLabelService;
import com.eams.service.TimelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final TimelineService timelineService;
    private final AssetHealthService healthService;
    private final QrLabelService qrLabelService;

    @PostMapping
    public ResponseEntity<AssetResponseDto> createAsset(@Valid @RequestBody AssetRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.createAsset(dto));
    }

    /**
     * All assets, or filtered: /api/assets?status=AVAILABLE&categoryId=2&q=dell
     * (filters can now be combined).
     */
    @GetMapping
    public ResponseEntity<List<AssetResponseDto>> getAssets(
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q) {
        if (status == null && categoryId == null && (q == null || q.isBlank())) {
            return ResponseEntity.ok(assetService.getAllAssets());
        }
        return ResponseEntity.ok(assetService.filterAssets(status, categoryId, q));
    }

    @GetMapping("/search")
    public ResponseEntity<List<AssetResponseDto>> searchAssets(@RequestParam String query) {
        return ResponseEntity.ok(assetService.searchAssets(query));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<AssetResponseDto>> getAssetsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(assetService.getAssetsByCategory(categoryId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<AssetResponseDto>> getAssetsByStatus(@PathVariable AssetStatus status) {
        return ResponseEntity.ok(assetService.getAssetsByStatus(status));
    }

    /** Warranties ending within N days (default 30). */
    @GetMapping("/warranty-expiring")
    public ResponseEntity<List<AssetResponseDto>> warrantyExpiring(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(assetService.getWarrantyExpiring(days));
    }

    /** Called by the mobile scan page (/a/{code}): asset + what this user may do with it. */
    @GetMapping("/code/{assetCode}")
    public ResponseEntity<AssetScanResponse> scan(@PathVariable String assetCode) {
        return ResponseEntity.ok(assetService.scanByCode(assetCode));
    }

    /** A4 sheet of QR stickers: /api/assets/labels/pdf?ids=1,2,3 (no ids = all assets). */
    @GetMapping("/labels/pdf")
    public ResponseEntity<byte[]> labels(@RequestParam(required = false) List<Long> ids) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=asset-labels.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(qrLabelService.labelsPdf(ids));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetResponseDto> getAssetById(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.getAssetById(id));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<TimelineEvent>> timeline(@PathVariable Long id) {
        return ResponseEntity.ok(timelineService.forAsset(id));
    }

    @GetMapping("/{id}/health")
    public ResponseEntity<AssetHealthResponse> health(@PathVariable Long id) {
        return ResponseEntity.ok(healthService.forAsset(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetResponseDto> updateAsset(
            @PathVariable Long id, @Valid @RequestBody AssetRequestDto dto) {
        return ResponseEntity.ok(assetService.updateAsset(id, dto));
    }

    @PutMapping("/{id}/retire")
    public ResponseEntity<AssetResponseDto> retireAsset(
            @PathVariable Long id, @Valid @RequestBody RetireAssetRequest request) {
        return ResponseEntity.ok(assetService.retireAsset(id, request));
    }

    /** Returns a plain-text message (kept as text for frontend compatibility). */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAsset(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.deleteAsset(id).message());
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadAssetImage(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(assetService.uploadAssetImage(id, file));
    }

    @GetMapping("/{id}/qr-code")
    public ResponseEntity<byte[]> generateQrCode(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=asset-" + id + "-qr.png")
                .contentType(MediaType.IMAGE_PNG)
                .body(assetService.generateQrCode(id));
    }
}
