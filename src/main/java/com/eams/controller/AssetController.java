package com.eams.controller;

import com.eams.dto.request.AssetRequestDto;
import com.eams.dto.response.AssetResponseDto;
import com.eams.entity.AssetStatus;
import com.eams.service.AssetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    public ResponseEntity<AssetResponseDto> createAsset(@Valid @RequestBody AssetRequestDto requestDto) {
        return new ResponseEntity<>(assetService.createAsset(requestDto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AssetResponseDto>> getAllAssets() {
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetResponseDto> getAssetById(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.getAssetById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetResponseDto> updateAsset(
            @PathVariable Long id,
            @Valid @RequestBody AssetRequestDto requestDto) {
        return ResponseEntity.ok(assetService.updateAsset(id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        return ResponseEntity.ok("Asset deleted successfully");
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
}