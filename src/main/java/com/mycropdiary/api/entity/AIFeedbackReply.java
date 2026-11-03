package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "\"AIFeedbackReply\"")
public class AIFeedbackReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_feedback_reply_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_feedback_id", nullable = false)
    private AIFeedback feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "reply_text", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String replyText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public AIFeedbackReply() {}

    public AIFeedbackReply(AIFeedback feedback, User user, String replyText) {
        this.feedback = feedback;
        this.user = user;
        this.replyText = replyText;
        this.createdAt = Instant.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public AIFeedback getFeedback() { return feedback; }
    public void setFeedback(AIFeedback feedback) { this.feedback = feedback; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getReplyText() { return replyText; }
    public void setReplyText(String replyText) { this.replyText = replyText; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

