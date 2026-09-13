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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;

    private final CategoryRepository categoryRepository;

    public AssetServiceImpl(
            AssetRepository assetRepository,
            CategoryRepository categoryRepository
    ) {
        this.assetRepository = assetRepository;
        this.categoryRepository = categoryRepository;
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Override
    public AssetResponseDto createAsset(
            AssetRequestDto requestDto
    ) {

        String assetCode =
                requestDto.getAssetCode().trim();

        if (assetRepository.existsByAssetCodeIgnoreCase(assetCode)) {

            throw new RuntimeException(
                    "Asset code already exists: " + assetCode
            );
        }

        String serialNumber =
                normalize(requestDto.getSerialNumber());

        if (serialNumber != null &&
                assetRepository.existsBySerialNumberIgnoreCase(
                        serialNumber
                )) {

            throw new RuntimeException(
                    "Serial number already exists: "
                            + serialNumber
            );
        }

        Category category =
                categoryRepository
                        .findById(requestDto.getCategoryId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Category not found with id: "
                                                + requestDto.getCategoryId()
                                )
                        );

        Asset asset = new Asset();

        asset.setAssetCode(assetCode);

        asset.setName(
                requestDto.getName().trim()
        );

        asset.setDescription(
                normalize(requestDto.getDescription())
        );

        asset.setSerialNumber(serialNumber);

        asset.setCategory(category);

        asset.setPurchaseDate(
                requestDto.getPurchaseDate()
        );

        asset.setPurchasePrice(
                requestDto.getPurchasePrice()
        );

        asset.setStatus(
                requestDto.getStatus() != null
                        ? requestDto.getStatus()
                        : AssetStatus.AVAILABLE
        );

        asset.setLocation(
                normalize(requestDto.getLocation())
        );

        Asset savedAsset =
                assetRepository.save(asset);

        return mapToResponseDto(savedAsset);
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponseDto> getAllAssets() {

        return assetRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public AssetResponseDto getAssetById(Long id) {

        Asset asset =
                assetRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Asset not found with id: "
                                                + id
                                )
                        );

        return mapToResponseDto(asset);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Override
    public AssetResponseDto updateAsset(
            Long id,
            AssetRequestDto requestDto
    ) {

        Asset asset =
                assetRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Asset not found with id: "
                                                + id
                                )
                        );

        String assetCode =
                requestDto.getAssetCode().trim();

        if (assetRepository
                .existsByAssetCodeIgnoreCaseAndIdNot(
                        assetCode,
                        id
                )) {

            throw new RuntimeException(
                    "Asset code already exists: "
                            + assetCode
            );
        }

        String serialNumber =
                normalize(requestDto.getSerialNumber());

        if (serialNumber != null &&
                assetRepository
                        .existsBySerialNumberIgnoreCaseAndIdNot(
                                serialNumber,
                                id
                        )) {

            throw new RuntimeException(
                    "Serial number already exists: "
                            + serialNumber
            );
        }

        Category category =
                categoryRepository
                        .findById(requestDto.getCategoryId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Category not found with id: "
                                                + requestDto.getCategoryId()
                                )
                        );

        asset.setAssetCode(assetCode);

        asset.setName(
                requestDto.getName().trim()
        );

        asset.setDescription(
                normalize(requestDto.getDescription())
        );

        asset.setSerialNumber(serialNumber);

        asset.setCategory(category);

        asset.setPurchaseDate(
                requestDto.getPurchaseDate()
        );

        asset.setPurchasePrice(
                requestDto.getPurchasePrice()
        );

        if (requestDto.getStatus() != null) {

            asset.setStatus(
                    requestDto.getStatus()
            );
        }

        asset.setLocation(
                normalize(requestDto.getLocation())
        );

        Asset updatedAsset =
                assetRepository.save(asset);

        return mapToResponseDto(updatedAsset);
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Override
    public void deleteAsset(Long id) {

        Asset asset =
                assetRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Asset not found with id: "
                                                + id
                                )
                        );

        assetRepository.delete(asset);
    }

    // =========================================================
    // SEARCH
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponseDto> searchAssets(
            String keyword
    ) {

        if (keyword == null ||
                keyword.trim().isEmpty()) {

            return getAllAssets();
        }

        String search = keyword.trim();

        return assetRepository
                .findByNameContainingIgnoreCaseOrAssetCodeContainingIgnoreCase(
                        search,
                        search
                )
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // =========================================================
    // BY CATEGORY
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponseDto> getAssetsByCategory(
            Long categoryId
    ) {

        if (!categoryRepository.existsById(categoryId)) {

            throw new RuntimeException(
                    "Category not found with id: "
                            + categoryId
            );
        }

        return assetRepository
                .findByCategoryId(categoryId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // =========================================================
    // BY STATUS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponseDto> getAssetsByStatus(
            AssetStatus status
    ) {

        if (status == null) {

            throw new RuntimeException(
                    "Asset status is required"
            );
        }

        return assetRepository
                .findByStatus(status)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // =========================================================
    // IMAGE UPLOAD
    // =========================================================

    @Override
    public String uploadAssetImage(
            Long id,
            MultipartFile file
    ) {

        Asset asset =
                assetRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Asset not found with id: "
                                                + id
                                )
                        );

        if (file == null || file.isEmpty()) {

            throw new RuntimeException(
                    "Image file is required"
            );
        }

        String contentType =
                file.getContentType();

        if (contentType == null ||
                !contentType.startsWith("image/")) {

            throw new RuntimeException(
                    "Only image files are allowed"
            );
        }

        try {

            String uploadDir =
                    "uploads/assets";

            Path uploadPath =
                    Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {

                Files.createDirectories(
                        uploadPath
                );
            }

            String originalName =
                    file.getOriginalFilename();

            String extension = "";

            if (originalName != null &&
                    originalName.contains(".")) {

                extension =
                        originalName.substring(
                                originalName.lastIndexOf(".")
                        );
            }

            String fileName =
                    UUID.randomUUID() + extension;

            Path filePath =
                    uploadPath.resolve(fileName);

            Files.copy(
                    file.getInputStream(),
                    filePath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            String imageUrl =
                    "/uploads/assets/" + fileName;

            asset.setImageUrl(imageUrl);

            assetRepository.save(asset);

            return imageUrl;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to store asset image",
                    e
            );
        }
    }

    // =========================================================
    // QR CODE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public byte[] generateQrCode(Long id) {

        Asset asset =
                assetRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Asset not found with id: "
                                                + id
                                )
                        );

        String qrContent =
                "Asset ID: " + asset.getId()
                        + "\nAsset Code: "
                        + asset.getAssetCode()
                        + "\nName: "
                        + asset.getName();

        try {

            QRCodeWriter qrCodeWriter =
                    new QRCodeWriter();

            BitMatrix bitMatrix =
                    qrCodeWriter.encode(
                            qrContent,
                            BarcodeFormat.QR_CODE,
                            250,
                            250
                    );

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            MatrixToImageWriter.writeToStream(
                    bitMatrix,
                    "PNG",
                    outputStream
            );

            return outputStream.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error generating QR code",
                    e
            );
        }
    }

    // =========================================================
    // MAPPER
    // =========================================================

    private AssetResponseDto mapToResponseDto(
            Asset asset
    ) {

        CategoryResponseDto categoryDto = null;

        if (asset.getCategory() != null) {

            categoryDto =
                    CategoryResponseDto.builder()
                            .id(
                                    asset.getCategory().getId()
                            )
                            .name(
                                    asset.getCategory().getName()
                            )
                            .description(
                                    asset.getCategory()
                                            .getDescription()
                            )
                            .build();
        }

        return AssetResponseDto.builder()
                .id(asset.getId())
                .assetCode(asset.getAssetCode())
                .name(asset.getName())
                .description(asset.getDescription())
                .serialNumber(asset.getSerialNumber())
                .category(categoryDto)
                .purchaseDate(asset.getPurchaseDate())
                .purchasePrice(asset.getPurchasePrice())
                .status(asset.getStatus())
                .location(asset.getLocation())
                .imageUrl(asset.getImageUrl())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .build();
    }

    // =========================================================
    // HELPER
    // =========================================================

    private String normalize(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }
}