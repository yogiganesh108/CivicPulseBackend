package com.example.backend.grievance;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "grievances")
public class Grievance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    @Column(length = 2000)
    private String description;
    private String category;
    private String location;
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] imageData;

    // content type of the image (e.g. image/png)
    private String imageType;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private Long userId;

    // officer assigned to this complaint (user id of officer)
    private Long officerId;

    // priority: Low/Medium/High
    private String priority;

    // deadline for resolution
    private LocalDate deadline;

    private Instant createdAt = Instant.now();

    @Column(length = 2000)
    private String resolutionNote;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] resolutionImageData;

    private String resolutionImageType;

    private Instant resolvedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
    public String getImageType() { return imageType; }
    public void setImageType(String imageType) { this.imageType = imageType; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getOfficerId() { return officerId; }
    public void setOfficerId(Long officerId) { this.officerId = officerId; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
    public byte[] getResolutionImageData() { return resolutionImageData; }
    public void setResolutionImageData(byte[] resolutionImageData) { this.resolutionImageData = resolutionImageData; }
    public String getResolutionImageType() { return resolutionImageType; }
    public void setResolutionImageType(String resolutionImageType) { this.resolutionImageType = resolutionImageType; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
