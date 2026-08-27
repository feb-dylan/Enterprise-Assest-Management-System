package com.eams.service;

import com.eams.dto.request.CreateAssetRequest;
import com.eams.dto.response.AssetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssetService {
    AssetResponse createAsset(CreateAssetRequest request);
    AssetResponse getAssetById(Long id);
    Page<AssetResponse> getAllAssets(Pageable pageable);
    void deleteAsset(Long id);
}