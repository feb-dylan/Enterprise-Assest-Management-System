package com.eams.repository;

import com.eams.entity.AuditEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository
        extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByAssetIdOrderByIdAsc(Long assetId);

    List<AuditEvent> findAllByOrderByIdDesc(Pageable pageable);
}
