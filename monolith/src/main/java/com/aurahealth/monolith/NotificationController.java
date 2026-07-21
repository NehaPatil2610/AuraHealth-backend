package com.aurahealth.monolith;

import com.aurahealth.monolith.entity.Notification;
import com.aurahealth.monolith.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationController(NotificationRepository notifications,
                                  UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    /**
     * GET /api/notifications — returns the authenticated user's notifications.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(Authentication authentication) {
        User user = currentUser(authentication);
        List<Notification> items = notifications.findByUserIdOrderByCreatedAtDesc(user.getId());
        long unreadCount = notifications.countByUserIdAndReadFalse(user.getId());
        return ResponseEntity.ok(Map.of(
                "notifications", items.stream().map(this::toView).toList(),
                "unreadCount", unreadCount
        ));
    }

    /**
     * PUT /api/notifications/{id}/read — marks a single notification as read.
     * Returns 404 if it doesn't belong to the authenticated user.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        return notifications.findByIdAndUserId(id, user.getId())
                .map(n -> {
                    n.setRead(true);
                    notifications.save(n);
                    return ResponseEntity.ok(Map.of("success", true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/notifications/read-all — marks all unread notifications as read.
     */
    @PutMapping("/read-all")
    @Transactional
    public ResponseEntity<?> markAllRead(Authentication authentication) {
        User user = currentUser(authentication);
        int updated = notifications.markAllReadByUserId(user.getId());
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    // ── Helpers ────────────────────────────────────────────────────

    private User currentUser(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
    }

    private Map<String, Object> toView(Notification n) {
        return Map.of(
                "id", n.getId(),
                "type", n.getType(),
                "title", n.getTitle(),
                "message", n.getMessage(),
                "read", n.isRead(),
                "createdAt", n.getCreatedAt().toString()
        );
    }
}
