package com.codesync.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer notificationId;

    private String recipientId;
    private String actorId;
    
    private String type;
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    private String relatedId;
    private String relatedType;
    
    @com.fasterxml.jackson.annotation.JsonProperty("isRead")
    private boolean isRead;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
