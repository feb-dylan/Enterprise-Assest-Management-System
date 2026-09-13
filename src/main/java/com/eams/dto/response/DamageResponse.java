package com.eams.dto.response;

import com.eams.entity.DamageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DamageResponse {

    private Long id;

    private Long assetId;

    private String assetCode;

    private String assetName;

    private Long employeeId;

    private String employeeCode;

    private String employeeName;

    private String description;

    private LocalDateTime reportedDate;

    private String evidenceUrl;

    private DamageStatus status;

    private String resolutionNote;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}