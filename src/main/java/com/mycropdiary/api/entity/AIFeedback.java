package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "\"AIFeedback\"")
public class AIFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_feedback_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_message_id", nullable = false)
    private AIMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "feedback_text", columnDefinition = "NVARCHAR(MAX)")
    private String feedbackText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AIFeedbackReply> replies = new ArrayList<>();

    public AIFeedback() {}

    public AIFeedback(AIMessage message, User user, Integer rating, String feedbackText) {
        this.message = message;
        this.user = user;
        this.rating = rating;
        this.feedbackText = feedbackText;
        this.createdAt = Instant.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public AIMessage getMessage() { return message; }
    public void setMessage(AIMessage message) { this.message = message; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getFeedbackText() { return feedbackText; }
    public void setFeedbackText(String feedbackText) { this.feedbackText = feedbackText; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public List<AIFeedbackReply> getReplies() { return replies; }
    public void setReplies(List<AIFeedbackReply> replies) { this.replies = replies; }
}

