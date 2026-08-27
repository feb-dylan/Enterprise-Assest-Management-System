package com.eams.mapper;

import com.eams.dto.request.CreateMaintenanceRequest;
import com.eams.dto.response.MaintenanceResponse;
import com.eams.entity.Maintenance;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceMapper {

    public Maintenance toEntity(CreateMaintenanceRequest request) {
        if (request == null) return null;

        Maintenance maintenance = new Maintenance();
        maintenance.setTechnicianName(request.getTechnicianName());
        maintenance.setMaintenanceType(request.getMaintenanceType());
        maintenance.setCost(request.getCost());
        maintenance.setStartDate(request.getStartDate());
        maintenance.setCompletionDate(request.getCompletionDate());
        maintenance.setRemarks(request.getRemarks());
        return maintenance;
    }

    public MaintenanceResponse toResponse(Maintenance entity) {
        if (entity == null) return null;

        MaintenanceResponse response = new MaintenanceResponse();
        response.setId(entity.getId());
        response.setWorkOrderNumber(entity.getWorkOrderNumber());

        if (entity.getAsset() != null) {
            response.setAssetId(entity.getAsset().getId());
            response.setAssetTag(entity.getAsset().getAssetTag());
            response.setAssetName(entity.getAsset().getName());
        }

        if (entity.getDamageReport() != null) {
            response.setDamageReportId(entity.getDamageReport().getId());
            response.setDamageReportNumber(entity.getDamageReport().getReportNumber());
        }

        response.setTechnicianName(entity.getTechnicianName());
        response.setMaintenanceType(entity.getMaintenanceType());
        response.setCost(entity.getCost());
        response.setStartDate(entity.getStartDate());
        response.setCompletionDate(entity.getCompletionDate());
        response.setStatus(entity.getStatus());
        response.setRemarks(entity.getRemarks());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }
}