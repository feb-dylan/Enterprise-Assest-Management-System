package com.eams.service;

import com.eams.dto.request.AssetRequestDto;
import com.eams.dto.response.AssetResponseDto;
import com.eams.entity.AssetStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AssetService {

    AssetResponseDto createAsset(
            AssetRequestDto requestDto
    );

    List<AssetResponseDto> getAllAssets();

    AssetResponseDto getAssetById(
            Long id
    );

    AssetResponseDto updateAsset(
            Long id,
            AssetRequestDto requestDto
    );

    void deleteAsset(Long id);

    List<AssetResponseDto> searchAssets(
            String keyword
    );

    List<AssetResponseDto> getAssetsByCategory(
            Long categoryId
    );

    List<AssetResponseDto> getAssetsByStatus(
            AssetStatus status
    );

    String uploadAssetImage(
            Long id,
            MultipartFile file
    );

    byte[] generateQrCode(
            Long id
    );
}