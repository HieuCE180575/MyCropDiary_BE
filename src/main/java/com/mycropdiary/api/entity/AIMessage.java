package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "\"AIMessage\"")
public class AIMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_message_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_conversation_id", nullable = false)
    private AIConversation conversation;

    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType; // user, assistant, system

    @Column(name = "message_text", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String messageText;

    @Column(name = "attachment_urls", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String attachmentUrls = "[]";

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public AIMessage() {}

    public AIMessage(AIConversation conversation, String senderType, String messageText, String modelName) {
        this.conversation = conversation;
        this.senderType = senderType;
        this.messageText = messageText;
        this.modelName = modelName;
        this.attachmentUrls = "[]";
        this.createdAt = Instant.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public AIConversation getConversation() { return conversation; }
    public void setConversation(AIConversation conversation) { this.conversation = conversation; }

    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }

    public String getAttachmentUrls() { return attachmentUrls; }
    public void setAttachmentUrls(String attachmentUrls) { this.attachmentUrls = attachmentUrls; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

