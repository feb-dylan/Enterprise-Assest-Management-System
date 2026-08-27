package com.eams.service.impl;

import com.eams.dto.request.AssetAssignmentRequest;
import com.eams.entity.Asset;
import com.eams.entity.AssetAssignment;
import com.eams.entity.AssignmentStatus;
import com.eams.entity.Employee;
import com.eams.entity.User;
import com.eams.repository.AssetAssignmentRepository;
import com.eams.repository.AssetRepository;
import com.eams.repository.EmployeeRepository;
import com.eams.repository.UserRepository;
import com.eams.service.AssetAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AssetAssignmentServiceImpl implements AssetAssignmentService {

    private final AssetAssignmentRepository assetAssignmentRepository;
    private final AssetRepository assetRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AssetAssignment createAssignment(AssetAssignmentRequest request) {
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> new RuntimeException("Asset not found with ID: " + request.getAssetId()));

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + request.getEmployeeId()));

        User assignedBy = null;
        if (request.getAssignedById() != null) {
            assignedBy = userRepository.findById(request.getAssignedById())
                    .orElseThrow(() -> new RuntimeException("User (assignedBy) not found with ID: " + request.getAssignedById()));
        }

        AssetAssignment assignment = new AssetAssignment();
        assignment.setAsset(asset);
        assignment.setEmployee(employee);
        assignment.setAssignedBy(assignedBy);
        
        // 1. Converts LocalDate from request to LocalDateTime via .atStartOfDay()
        LocalDate reqAssignedDate = request.getAssignedDate() != null ? request.getAssignedDate() : LocalDate.now();
        assignment.setAssignedDate(reqAssignedDate.atStartOfDay());

        // 2. Direct LocalDate mapping for return dates
        assignment.setExpectedReturnDate(request.getExpectedReturnDate());
        assignment.setActualReturnDate(request.getActualReturnDate());

        assignment.setNotes(request.getNotes());
        
        // 3. Converts string status to com.eams.entity.AssignmentStatus enum
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            assignment.setStatus(AssignmentStatus.valueOf(request.getStatus().toUpperCase()));
        }

        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());

        return assetAssignmentRepository.save(assignment);
    }
}