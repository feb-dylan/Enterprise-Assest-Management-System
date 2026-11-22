package com.eams.service;

import com.eams.dto.request.AssetRequestDto;
import com.eams.dto.request.Requests.RetireAssetRequest;
import com.eams.dto.response.AssetResponseDto;
import com.eams.dto.response.Responses.AssetScanResponse;
import com.eams.dto.response.Responses.MessageResponse;
import com.eams.entity.AssetStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AssetService {

    AssetResponseDto createAsset(AssetRequestDto requestDto);

    List<AssetResponseDto> getAllAssets();

    /** Combined filter; every argument is optional. */
    List<AssetResponseDto> filterAssets(AssetStatus status, Long categoryId, String query);

    AssetResponseDto getAssetById(Long id);

    AssetResponseDto updateAsset(Long id, AssetRequestDto requestDto);

    /** Hard-deletes an asset without history; retires it otherwise. */
    MessageResponse deleteAsset(Long id);

    AssetResponseDto retireAsset(Long id, RetireAssetRequest request);

    List<AssetResponseDto> searchAssets(String keyword);

    List<AssetResponseDto> getAssetsByCategory(Long categoryId);

    List<AssetResponseDto> getAssetsByStatus(AssetStatus status);

    List<AssetResponseDto> getWarrantyExpiring(int days);

    String uploadAssetImage(Long id, MultipartFile file);

    byte[] generateQrCode(Long id);

    /** What the scanned QR code shows to the current user. */
    AssetScanResponse scanByCode(String assetCode);
}
