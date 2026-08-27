package com.eams.mapper;

import com.eams.dto.request.CreateAssetRequest;
import com.eams.dto.response.AssetResponse;
import com.eams.entity.Asset;
import org.springframework.stereotype.Component;

@Component
public class AssetMapper {

    public Asset toEntity(CreateAssetRequest request) {
        if (request == null) return null;

        Asset asset = new Asset();
        asset.setAssetTag(request.getAssetTag());
        asset.setName(request.getName());
        asset.setSerialNumber(request.getSerialNumber());
        asset.setPurchaseDate(request.getPurchaseDate());
        asset.setPurchaseCost(request.getPurchaseCost());
        asset.setSpecification(request.getSpecification());
        return asset;
    }

    public AssetResponse toResponse(Asset entity) {
        if (entity == null) return null;

        AssetResponse response = new AssetResponse();
        response.setId(entity.getId());
        response.setAssetTag(entity.getAssetTag());
        response.setName(entity.getName());
        response.setSerialNumber(entity.getSerialNumber());
        if (entity.getCategory() != null) {
            response.setCategoryId(entity.getCategory().getId());
            response.setCategoryName(entity.getCategory().getName());
        }
        if (entity.getDepartment() != null) {
            response.setDepartmentId(entity.getDepartment().getId());
            response.setDepartmentName(entity.getDepartment().getName());
        }
        response.setPurchaseDate(entity.getPurchaseDate());
        response.setPurchaseCost(entity.getPurchaseCost());
        response.setStatus(entity.getStatus());
        response.setQrCodeUrl(entity.getQrCodeUrl());
        response.setImageUrl(entity.getImageUrl());
        response.setSpecification(entity.getSpecification());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}