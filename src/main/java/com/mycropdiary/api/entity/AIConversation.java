package com.mycropdiary.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "\"AIConversation\"")
public class AIConversation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_conversation_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @Column(name = "conversation_title", length = 200)
    private String conversationTitle;

    @Column(name = "conversation_status", nullable = false, length = 20)
    private String conversationStatus = "open";

    public AIConversation() {}

    public AIConversation(User user, Farm farm, String conversationTitle) {
        this.user = user;
        this.farm = farm;
        this.conversationTitle = conversationTitle;
        this.conversationStatus = "open";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public String getConversationTitle() { return conversationTitle; }
    public void setConversationTitle(String conversationTitle) { this.conversationTitle = conversationTitle; }

    public String getConversationStatus() { return conversationStatus; }
    public void setConversationStatus(String conversationStatus) { this.conversationStatus = conversationStatus; }
}

