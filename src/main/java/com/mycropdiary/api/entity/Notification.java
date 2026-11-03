package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "\"Notification\"")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String message;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType = "SYSTEM";

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Notification() {}

    public Notification(User user, String title, String message, String notificationType, String actionUrl) {
        this.user = user;
        this.title = title;
        this.message = message;
        this.notificationType = notificationType != null ? notificationType : "SYSTEM";
        this.actionUrl = actionUrl;
        this.isRead = false;
        this.createdAt = Instant.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

