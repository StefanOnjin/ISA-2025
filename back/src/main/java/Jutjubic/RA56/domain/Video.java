package Jutjubic.RA56.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String tags;

    @Column(nullable = true)
    private Long views = 0L;

    @Column(nullable = false)
    private String thumbnailPath;

    @Column(nullable = false)
    private String videoPath;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime scheduledAt;

    @Column(nullable = true)
    private Long durationSeconds;

    @Column(nullable = true)
    private Boolean premiereEnabled;

    @Column(nullable = true)
    private Boolean thumbnailCompressed = false;

    @Column(nullable = true)
    private LocalDateTime thumbnailCompressedAt;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    public Video() {
    }

    public Video(String title, String description, String tags, String thumbnailPath, String videoPath, LocalDateTime createdAt, LocalDateTime scheduledAt, Long durationSeconds, Boolean premiereEnabled, Double latitude, Double longitude, User owner) {
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.thumbnailPath = thumbnailPath;
        this.videoPath = videoPath;
        this.createdAt = createdAt;
        this.scheduledAt = scheduledAt;
        this.durationSeconds = durationSeconds;
        this.premiereEnabled = premiereEnabled;
        this.thumbnailCompressed = false;
        this.thumbnailCompressedAt = null;
        this.latitude = latitude;
        this.longitude = longitude;
        this.owner = owner;
    }

    public Long getViews() {
        return views;
    }

    public void setViews(Long views) {
        this.views = views;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Boolean getPremiereEnabled() {
        return premiereEnabled;
    }

    public void setPremiereEnabled(Boolean premiereEnabled) {
        this.premiereEnabled = premiereEnabled;
    }

    public Boolean getThumbnailCompressed() {
        return thumbnailCompressed;
    }

    public void setThumbnailCompressed(Boolean thumbnailCompressed) {
        this.thumbnailCompressed = thumbnailCompressed;
    }

    public LocalDateTime getThumbnailCompressedAt() {
        return thumbnailCompressedAt;
    }

    public void setThumbnailCompressedAt(LocalDateTime thumbnailCompressedAt) {
        this.thumbnailCompressedAt = thumbnailCompressedAt;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
