package com.example.backend.grievance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByGrievanceIdOrderByCreatedAtDesc(Long grievanceId);
    boolean existsByGrievanceIdAndUserId(Long grievanceId, Long userId);
    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.grievanceId = :grievanceId")
    Double findAverageRatingByGrievanceId(@Param("grievanceId") Long grievanceId);

    long countByGrievanceId(Long grievanceId);
}
