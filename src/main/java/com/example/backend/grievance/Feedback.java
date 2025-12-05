package com.example.backend.grievance;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "feedback")
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long grievanceId;

    private Long userId;

    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comments;

    private Instant createdAt = Instant.now();

    private boolean reopened = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGrievanceId() { return grievanceId; }
    public void setGrievanceId(Long grievanceId) { this.grievanceId = grievanceId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isReopened() { return reopened; }
    public void setReopened(boolean reopened) { this.reopened = reopened; }
}
