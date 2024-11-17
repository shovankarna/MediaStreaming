package com.media.MediaStreaming.Models;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "media_access_log")
public class MediaAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accessId;

    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "media_id", referencedColumnName = "mediaId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Media media;

    @Column(nullable = false)
    private String externalUserId;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime accessTime = LocalDateTime.now();

    private String ipAddress;

    @Enumerated(EnumType.STRING)
    private Action action;

}