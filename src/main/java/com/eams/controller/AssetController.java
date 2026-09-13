package com.eams.controller;

import com.eams.dto.request.AssetRequestDto;
import com.eams.dto.response.AssetResponseDto;
import com.eams.entity.AssetStatus;
import com.eams.service.AssetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    // =========================================================
    // CREATE
    // =========================================================

    @PostMapping
    public ResponseEntity<AssetResponseDto> createAsset(
            @Valid @RequestBody AssetRequestDto requestDto) {

        AssetResponseDto response =
                assetService.createAsset(requestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @GetMapping
    public ResponseEntity<List<AssetResponseDto>> getAllAssets() {
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    // =========================================================
    // SEARCH
    // =========================================================

    @GetMapping("/search")
    public ResponseEntity<List<AssetResponseDto>> searchAssets(
            @RequestParam String query) {

        return ResponseEntity.ok(
                assetService.searchAssets(query)
        );
    }

    // =========================================================
    // BY CATEGORY
    // =========================================================

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<AssetResponseDto>> getAssetsByCategory(
            @PathVariable Long categoryId) {

        return ResponseEntity.ok(
                assetService.getAssetsByCategory(categoryId)
        );
    }

    // =========================================================
    // BY STATUS
    // =========================================================

    @GetMapping("/status/{status}")
    public ResponseEntity<List<AssetResponseDto>> getAssetsByStatus(
            @PathVariable AssetStatus status) {

        return ResponseEntity.ok(
                assetService.getAssetsByStatus(status)
        );
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<AssetResponseDto> getAssetById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                assetService.getAssetById(id)
        );
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<AssetResponseDto> updateAsset(
            @PathVariable Long id,
            @Valid @RequestBody AssetRequestDto requestDto) {

        return ResponseEntity.ok(
                assetService.updateAsset(id, requestDto)
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAsset(
            @PathVariable Long id) {

        assetService.deleteAsset(id);

        return ResponseEntity.ok(
                "Asset deleted successfully"
        );
    }

    // =========================================================
    // UPLOAD IMAGE
    // =========================================================

    @PostMapping(
            value = "/{id}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> uploadAssetImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        String imageUrl =
                assetService.uploadAssetImage(id, file);

        return ResponseEntity.ok(imageUrl);
    }

    // =========================================================
    // GENERATE QR CODE
    // =========================================================

    @GetMapping("/{id}/qr-code")
    public ResponseEntity<byte[]> generateQrCode(
            @PathVariable Long id) {

        byte[] qrCode =
                assetService.generateQrCode(id);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=asset-" + id + "-qr.png"
                )
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode);
    }
}