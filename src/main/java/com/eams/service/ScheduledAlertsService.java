package com.eams.service;

import com.eams.entity.Asset;
import com.eams.entity.AssetRequest;
import com.eams.entity.RequestStatus;
import com.eams.entity.User;
import com.eams.repository.AssetRepository;
import com.eams.repository.AssetRequestRepository;
import com.eams.repository.NotificationRepository;
import com.eams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Daily automatic alerts (08:00 server time):
 *  - warranties expiring in the next 30 days -> admins + managers
 *  - approved requests still not assigned after 3 days -> admins
 *
 * The same alert is not repeated within 7 days.
 */
@Service
@RequiredArgsConstructor
public class ScheduledAlertsService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledAlertsService.class);

    private final AssetRepository assetRepository;
    private final AssetRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void runDailyAlerts() {
        int warranty = warrantyAlerts();
        int stale = staleApprovedRequestAlerts();
        log.info("Daily alerts: {} warranty, {} unassigned-request notifications", warranty, stale);
    }

    @Transactional
    public int warrantyAlerts() {
        LocalDate today = LocalDate.now();
        List<Asset> expiring = assetRepository.findWarrantyExpiringBetween(today, today.plusDays(30));
        int sent = 0;
        for (String role : List.of("ADMIN", "MANAGER")) {
            for (User user : userRepository.findByRoleName(role)) {
                for (Asset asset : expiring) {
                    String link = "/assets/" + asset.getId();
                    if (alreadySent(user, "WARRANTY_EXPIRING", link)) {
                        continue;
                    }
                    long days = ChronoUnit.DAYS.between(today, asset.getWarrantyExpiryDate());
                    notificationService.notify(user, "WARRANTY_EXPIRING",
                            "Warranty expiring in " + days + " day(s)",
                            asset.getAssetCode() + " - " + asset.getName()
                                    + ": warranty ends " + asset.getWarrantyExpiryDate() + ".",
                            link);
                    sent++;
                }
            }
        }
        return sent;
    }

    @Transactional
    public int staleApprovedRequestAlerts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(3);
        int sent = 0;
        for (AssetRequest request : requestRepository.findByStatus(RequestStatus.APPROVED)) {
            if (request.getApprovedAt() == null || request.getApprovedAt().isAfter(cutoff)) {
                continue;
            }
            for (User admin : userRepository.findByRoleName("ADMIN")) {
                String link = "/requests/" + request.getId();
                if (alreadySent(admin, "REQUEST_OVERDUE", link)) {
                    continue;
                }
                notificationService.notify(admin, "REQUEST_OVERDUE",
                        "Approved request waiting for assignment",
                        "Request #" + request.getId() + " was approved on "
                                + request.getApprovedAt().toLocalDate() + " and is still not assigned.",
                        link);
                sent++;
            }
        }
        return sent;
    }

    private boolean alreadySent(User user, String type, String link) {
        return notificationRepository.existsByUserIdAndTypeAndLinkAndCreatedAtAfter(
                user.getId(), type, link, LocalDateTime.now().minusDays(7));
    }
}
