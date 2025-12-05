package com.example.backend.grievance;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedbackService {
    private final FeedbackRepository repo;
    private final GrievanceService grievanceService;

    public FeedbackService(FeedbackRepository repo, GrievanceService grievanceService){
        this.repo = repo;
        this.grievanceService = grievanceService;
    }

    public Feedback saveFeedback(Feedback f){
        // Prevent duplicate feedback (one feedback per user per grievance)
        if(f.getGrievanceId() != null && f.getUserId() != null){
            boolean exists = repo.existsByGrievanceIdAndUserId(f.getGrievanceId(), f.getUserId());
            if(exists){
                throw new IllegalStateException("Feedback already submitted for this grievance by the user");
            }
        }
        return repo.save(f);
    }

    public List<Feedback> findByGrievance(Long grievanceId){
        return repo.findByGrievanceIdOrderByCreatedAtDesc(grievanceId);
    }

    public List<Feedback> findAll(){ return repo.findAll(); }

    public Double getAverageRatingForGrievance(Long grievanceId){
        Double avg = repo.findAverageRatingByGrievanceId(grievanceId);
        return avg; // may be null if no ratings
    }

    public long getRatingCountForGrievance(Long grievanceId){
        return repo.countByGrievanceId(grievanceId);
    }
}