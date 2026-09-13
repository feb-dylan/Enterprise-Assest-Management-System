package com.eams.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    // =========================================================
    // 1. Request APPROVED → employee
    // =========================================================

    public void sendRequestApprovedNotification(
            String to,
            String assetName
    ) {
        send(
                to,
                "Asset Request Approved",
                "Good news!\n\n" +
                        "Your request for '" + assetName + "' has been APPROVED.\n" +
                        "An administrator will assign the asset to you shortly.\n\n" +
                        "— EAMS"
        );
    }

    // =========================================================
    // 2. Request REJECTED → employee
    // =========================================================

    public void sendRequestRejectedNotification(
            String to,
            String assetName,
            String rejectionReason
    ) {
        send(
                to,
                "Asset Request Rejected",
                "Your request for '" + assetName + "' has been REJECTED.\n\n" +
                        "Reason: " + rejectionReason + "\n\n" +
                        "— EAMS"
        );
    }

    // =========================================================
    // 3. Asset ASSIGNED → employee
    // =========================================================

    public void sendAssetAssignedNotification(
            String to,
            String assetName,
            String assetCode
    ) {
        send(
                to,
                "Asset Assigned to You",
                "An asset has been assigned to you.\n\n" +
                        "Asset: " + assetName + "\n" +
                        "Code:  " + assetCode + "\n\n" +
                        "Please contact your administrator if you have any questions.\n\n" +
                        "— EAMS"
        );
    }

    // =========================================================
    // 4. Asset RETURNED → employee
    // =========================================================

    public void sendAssetReturnedNotification(
            String to,
            String assetName,
            String assetCode
    ) {
        send(
                to,
                "Asset Returned",
                "Your assignment of the following asset has been marked as RETURNED.\n\n" +
                        "Asset: " + assetName + "\n" +
                        "Code:  " + assetCode + "\n\n" +
                        "Thank you.\n\n" +
                        "— EAMS"
        );
    }

    // =========================================================
    // PRIVATE HELPER
    // =========================================================

    private void send(String to, String subject, String body) {

        if (to == null || to.isBlank()) {
            System.err.println(
                    "Notification skipped: recipient email is empty"
            );
            return;
        }

        try {

            SimpleMailMessage message =
                    new SimpleMailMessage();

            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            System.out.println(
                    "Notification sent to " + to + " — " + subject
            );

        } catch (Exception e) {

            // Log but never break the business flow
            System.err.println(
                    "Failed to send notification to " + to + ": " + e.getMessage()
            );
        }
    }
}