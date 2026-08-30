package com.eams.service.impl;

import com.eams.dto.request.AssetRequestDto;
import com.eams.dto.response.AssetResponseDto;
import com.eams.dto.response.CategoryResponseDto;
import com.eams.entity.Asset;
import com.eams.entity.AssetStatus;
import com.eams.entity.Category;
import com.eams.repository.AssetRepository;
import com.eams.repository.CategoryRepository;
import com.eams.service.AssetService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final CategoryRepository categoryRepository;

    public AssetServiceImpl(AssetRepository assetRepository, CategoryRepository categoryRepository) {
        this.assetRepository = assetRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public AssetResponseDto createAsset(AssetRequestDto requestDto) {
        if (assetRepository.existsByAssetCode(requestDto.getAssetCode())) {
            throw new RuntimeException("Asset code already exists: " + requestDto.getAssetCode());
        }

        Category category = categoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + requestDto.getCategoryId()));

        Asset asset = new Asset();
        asset.setAssetCode(requestDto.getAssetCode());
        asset.setName(requestDto.getName());
        asset.setDescription(requestDto.getDescription());
        asset.setSerialNumber(requestDto.getSerialNumber());
        asset.setCategory(category);
        asset.setPurchaseDate(requestDto.getPurchaseDate());
        asset.setPurchasePrice(requestDto.getPurchasePrice());
        asset.setStatus(requestDto.getStatus());
        asset.setLocation(requestDto.getLocation());

        Asset savedAsset = assetRepository.save(asset);
        return mapToResponseDto(savedAsset);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponseDto> getAllAssets() {
        return assetRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponseDto getAssetById(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found with id: " + id));
        return mapToResponseDto(asset);
    }

    @Override
    @Transactional
    public AssetResponseDto updateAsset(Long id, AssetRequestDto requestDto) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found with id: " + id));

        Category category = categoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + requestDto.getCategoryId()));

        // Fixed: Ensure assetCode is maintained/updated
        asset.setAssetCode(requestDto.getAssetCode());
        asset.setName(requestDto.getName());
        asset.setDescription(requestDto.getDescription());
        asset.setSerialNumber(requestDto.getSerialNumber());
        asset.setCategory(category);
        asset.setPurchaseDate(requestDto.getPurchaseDate());
        asset.setPurchasePrice(requestDto.getPurchasePrice());
        asset.setStatus(requestDto.getStatus());
        asset.setLocation(requestDto.getLocation());

        Asset updatedAsset = assetRepository.save(asset);
        return mapToResponseDto(updatedAsset);
    }

    @Override
    @Transactional
    public void deleteAsset(Long id) {
        if (!assetRepository.existsById(id)) {
            throw new RuntimeException("Asset not found with id: " + id);
        }
        assetRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponseDto> searchAssets(String keyword) {
        return assetRepository.findAll().stream()
                .filter(a -> (a.getName() != null && a.getName().toLowerCase().contains(keyword.toLowerCase())) ||
                        (a.getAssetCode() != null && a.getAssetCode().toLowerCase().contains(keyword.toLowerCase())))
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponseDto> getAssetsByCategory(Long categoryId) {
        return assetRepository.findByCategoryId(categoryId).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponseDto> getAssetsByStatus(AssetStatus status) {
        return assetRepository.findByStatus(status).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String uploadAssetImage(Long id, MultipartFile file) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found with id: " + id));

        try {
            String uploadDir = "uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            asset.setImageUrl("/" + uploadDir + fileName);
            assetRepository.save(asset);
            return asset.getImageUrl();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store image file", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateQrCode(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found with id: " + id));

        String qrContent = "Asset Code: " + asset.getAssetCode() + "\nName: " + asset.getName();
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    qrContent,
                    BarcodeFormat.QR_CODE,
                    250, 250
            );

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating QR code", e);
        }
    }

    private AssetResponseDto mapToResponseDto(Asset asset) {
        AssetResponseDto responseDto = new AssetResponseDto();
        responseDto.setId(asset.getId());
        responseDto.setAssetCode(asset.getAssetCode());
        responseDto.setName(asset.getName());
        responseDto.setDescription(asset.getDescription());
        responseDto.setSerialNumber(asset.getSerialNumber());
        responseDto.setPurchaseDate(asset.getPurchaseDate());
        responseDto.setPurchasePrice(asset.getPurchasePrice());
        responseDto.setStatus(asset.getStatus());
        responseDto.setLocation(asset.getLocation());
        responseDto.setImageUrl(asset.getImageUrl());
        responseDto.setCreatedAt(asset.getCreatedAt());
        responseDto.setUpdatedAt(asset.getUpdatedAt());

        if (asset.getCategory() != null) {
            CategoryResponseDto categoryDto = new CategoryResponseDto();
            categoryDto.setId(asset.getCategory().getId());
            categoryDto.setName(asset.getCategory().getName());
            categoryDto.setDescription(asset.getCategory().getDescription());
            responseDto.setCategory(categoryDto);
        }

        return responseDto;
    }
}