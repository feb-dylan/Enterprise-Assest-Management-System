package com.eams.mapper;

import com.eams.dto.request.CreateDamageReportRequest;
import com.eams.dto.response.DamageReportResponse;
import com.eams.entity.DamageReport;
import org.springframework.stereotype.Component;

@Component
public class DamageReportMapper {

    public DamageReport toEntity(CreateDamageReportRequest request) {
        if (request == null) return null;

        DamageReport report = new DamageReport();
        report.setDescription(request.getDescription());
        report.setIncidentDate(request.getIncidentDate());
        report.setImageUrl(request.getImageUrl());
        return report;
    }

    public DamageReportResponse toResponse(DamageReport entity) {
        if (entity == null) return null;

        DamageReportResponse response = new DamageReportResponse();
        response.setId(entity.getId());
        response.setReportNumber(entity.getReportNumber());

        if (entity.getAsset() != null) {
            response.setAssetId(entity.getAsset().getId());
            response.setAssetTag(entity.getAsset().getAssetTag());
            response.setAssetName(entity.getAsset().getName());
        }

        if (entity.getReportedByEmployee() != null) {
            response.setReportedByEmployeeId(entity.getReportedByEmployee().getId());
            response.setReportedByEmployeeName(
                    entity.getReportedByEmployee().getFirstName() + " " + entity.getReportedByEmployee().getLastName()
            );
        }

        response.setDescription(entity.getDescription());
        response.setIncidentDate(entity.getIncidentDate());
        response.setStatus(entity.getStatus());
        response.setImageUrl(entity.getImageUrl());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}