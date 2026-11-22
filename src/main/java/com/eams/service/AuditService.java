package com.eams.service;

import com.eams.entity.AuditEvent;
import com.eams.repository.AuditEventRepository;
import com.eams.security.AppUserPrincipal;
import com.eams.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Writes the append-only audit log (who did what, when).
 * Runs inside the caller's transaction, so if the business change
 * rolls back, the audit entry rolls back with it.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final CurrentUserService currentUserService;

    public void record(Long assetId, String entityType, Long entityId,
                       String action, String details) {
        AuditEvent event = new AuditEvent();
        event.setAssetId(assetId);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setAction(action);
        event.setDetails(details != null && details.length() > 1000
                ? details.substring(0, 1000)
                : details);

        currentUserService.optionalPrincipal().ifPresent((AppUserPrincipal p) -> {
            event.setActorUserId(p.id());
            event.setActorEmail(p.email());
        });

        auditEventRepository.save(event);
    }
}
