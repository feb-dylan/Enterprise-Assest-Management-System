package com.eams.service.impl;

import com.eams.dto.request.MaintenanceCreateRequest;
import com.eams.dto.request.MaintenanceUpdateRequest;
import com.eams.dto.response.MaintenanceResponse;

import com.eams.entity.Asset;
import com.eams.entity.AssetStatus;
import com.eams.entity.Maintenance;
import com.eams.entity.MaintenanceStatus;

import com.eams.repository.AssetRepository;
import com.eams.repository.MaintenanceRepository;

import com.eams.service.MaintenanceService;

import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceServiceImpl
        implements MaintenanceService {


    private final MaintenanceRepository maintenanceRepository;

    private final AssetRepository assetRepository;


    // =========================================================
    // CREATE MAINTENANCE
    // =========================================================

    @Override
    public MaintenanceResponse createMaintenance(
            MaintenanceCreateRequest request
    ) {

        Asset asset =
                assetRepository.findById(
                        request.getAssetId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Asset not found"
                        )
                );


        if (asset.getStatus() == AssetStatus.RETIRED) {

            throw new RuntimeException(
                    "Cannot create maintenance for a retired asset"
            );
        }


        Maintenance maintenance =
                new Maintenance();

        maintenance.setAsset(asset);

        maintenance.setTechnician(
                request.getTechnician()
        );

        maintenance.setDescription(
                request.getDescription()
        );

        maintenance.setStartDate(
                request.getStartDate()
        );

        maintenance.setEndDate(
                request.getEndDate()
        );

        maintenance.setRepairCost(
                request.getRepairCost()
        );

        maintenance.setNotes(
                request.getNotes()
        );

        maintenance.setStatus(
                MaintenanceStatus.SCHEDULED
        );


        Maintenance saved =
                maintenanceRepository.save(
                        maintenance
                );


        // Asset goes into maintenance
        asset.setStatus(
                AssetStatus.MAINTENANCE
        );

        assetRepository.save(asset);


        return mapToResponse(saved);
    }


    // =========================================================
    // GET ALL
    // =========================================================

    @Override
    public List<MaintenanceResponse>
    getAllMaintenance() {

        return maintenanceRepository
                .findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET ONE
    // =========================================================

    @Override
    public MaintenanceResponse getMaintenanceById(
            Long id
    ) {

        Maintenance maintenance =
                maintenanceRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Maintenance record not found"
                                )
                        );


        return mapToResponse(maintenance);
    }


    // =========================================================
    // GET BY ASSET
    // =========================================================

    @Override
    public List<MaintenanceResponse>
    getMaintenanceByAsset(
            Long assetId
    ) {

        if (!assetRepository.existsById(assetId)) {

            throw new RuntimeException(
                    "Asset not found"
            );
        }


        return maintenanceRepository
                .findByAssetId(assetId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // UPDATE
    // =========================================================

    @Override
    public MaintenanceResponse updateMaintenance(
            Long id,
            MaintenanceUpdateRequest request
    ) {

        Maintenance maintenance =
                maintenanceRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Maintenance record not found"
                                )
                        );


        if (request.getStatus() != null) {

            maintenance.setStatus(
                    request.getStatus()
            );
        }


        if (request.getEndDate() != null) {

            maintenance.setEndDate(
                    request.getEndDate()
            );
        }


        if (request.getRepairCost() != null) {

            maintenance.setRepairCost(
                    request.getRepairCost()
            );
        }


        if (request.getTechnician() != null) {

            maintenance.setTechnician(
                    request.getTechnician()
            );
        }


        if (request.getNotes() != null) {

            maintenance.setNotes(
                    request.getNotes()
            );
        }


        Asset asset =
                maintenance.getAsset();


        // =====================================================
        // UPDATE ASSET STATUS BASED ON MAINTENANCE STATUS
        // =====================================================

        MaintenanceStatus status =
                maintenance.getStatus();


        switch (status) {

            case SCHEDULED:
            case IN_PROGRESS:

                asset.setStatus(
                        AssetStatus.MAINTENANCE
                );

                break;


            case COMPLETED:

                asset.setStatus(
                        AssetStatus.AVAILABLE
                );

                if (maintenance.getEndDate() == null) {

                    maintenance.setEndDate(
                            java.time.LocalDate.now()
                    );
                }

                break;


            case CANCELLED:

                asset.setStatus(
                        AssetStatus.AVAILABLE
                );

                break;
        }


        assetRepository.save(asset);


        Maintenance saved =
                maintenanceRepository.save(
                        maintenance
                );


        return mapToResponse(saved);
    }


    // =========================================================
    // MAPPING
    // =========================================================

    private MaintenanceResponse mapToResponse(
            Maintenance maintenance
    ) {

        Long assetId = null;
        String assetCode = null;
        String assetName = null;


        if (maintenance.getAsset() != null) {

            assetId =
                    maintenance.getAsset().getId();

            assetCode =
                    maintenance.getAsset().getAssetCode();

            assetName =
                    maintenance.getAsset().getName();
        }


        return MaintenanceResponse.builder()

                .id(maintenance.getId())

                .assetId(assetId)

                .assetCode(assetCode)

                .assetName(assetName)

                .technician(
                        maintenance.getTechnician()
                )

                .description(
                        maintenance.getDescription()
                )

                .startDate(
                        maintenance.getStartDate()
                )

                .endDate(
                        maintenance.getEndDate()
                )

                .repairCost(
                        maintenance.getRepairCost()
                )

                .status(
                        maintenance.getStatus()
                )

                .notes(
                        maintenance.getNotes()
                )

                .createdAt(
                        maintenance.getCreatedAt()
                )

                .updatedAt(
                        maintenance.getUpdatedAt()
                )

                .build();
    }
}