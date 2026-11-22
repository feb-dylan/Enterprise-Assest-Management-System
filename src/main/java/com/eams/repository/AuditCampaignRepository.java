package com.eams.repository;

import com.eams.entity.AuditCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditCampaignRepository
        extends JpaRepository<AuditCampaign, Long> {

    List<AuditCampaign> findAllByOrderByIdDesc();
}
