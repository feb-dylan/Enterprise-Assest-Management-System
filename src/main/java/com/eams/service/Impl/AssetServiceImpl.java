package com.eams.service.impl;

import com.eams.dto.request.CreateAssetRequest;
import com.eams.dto.response.AssetResponse;
import com.eams.entity.Asset;
import com.eams.entity.AssetStatus;
import com.eams.entity.Category;
import com.eams.entity.Department;
import com.eams.exception.ResourceNotFoundException;
import com.eams.mapper.AssetMapper;
import com.eams.repository.AssetRepository;
import com.eams.repository.CategoryRepository;
import com.eams.repository.DepartmentRepository;
import com.eams.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final AssetMapper assetMapper;

    @Override
    @Transactional
    public AssetResponse createAsset(CreateAssetRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
        }

        Asset asset = assetMapper.toEntity(request);
        asset.setCategory(category);
        asset.setDepartment(department);
        asset.setStatus(AssetStatus.AVAILABLE);

        Asset savedAsset = assetRepository.save(asset);
        return assetMapper.toResponse(savedAsset);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponse getAssetById(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", id));
        return assetMapper.toResponse(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponse> getAllAssets(Pageable pageable) {
        return assetRepository.findAll(pageable).map(assetMapper::toResponse);
    }

    @Override
    @Transactional
    public void deleteAsset(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", id));
        assetRepository.delete(asset);
    }
}