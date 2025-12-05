package com.example.backend.grievance;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;
    private final UserRepository userRepository;
    private final GrievanceService grievanceService;

    public FeedbackController(FeedbackService feedbackService, UserRepository userRepository, GrievanceService grievanceService){
        this.feedbackService = feedbackService;
        this.userRepository = userRepository;
        this.grievanceService = grievanceService;
    }

    @PostMapping
    public ResponseEntity<?> submitFeedback(@RequestBody Map<String,Object> body, Authentication authentication){
        try{
            String username = authentication.getName();
            User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
            Long grievanceId = body.containsKey("grievanceId") ? Long.valueOf(body.get("grievanceId").toString()) : null;
            if(grievanceId == null) return ResponseEntity.badRequest().body(Map.of("error","grievanceId required"));
            // Only allow feedback when grievance is RESOLVED
            var maybe = grievanceService.findById(grievanceId);
            if(maybe.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","Grievance not found"));
            var g = maybe.get();
            if(g.getStatus() == null || !"RESOLVED".equalsIgnoreCase(g.getStatus().name())){
                return ResponseEntity.badRequest().body(Map.of("error","Feedback allowed only for resolved grievances"));
            }
            int rating = body.containsKey("rating") ? Integer.parseInt(body.get("rating").toString()) : 0;
            String comments = body.containsKey("comments") ? (String) body.get("comments") : null;
            Feedback f = new Feedback();
            f.setGrievanceId(grievanceId);
            f.setUserId(user.getId());
            f.setRating(rating);
            f.setComments(comments);
            f.setCreatedAt(Instant.now());
            Feedback saved = feedbackService.saveFeedback(f);
            return ResponseEntity.ok(Map.of("message","saved","id", saved.getId()));
        }catch(IllegalStateException ex){
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        }catch(Exception ex){
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/complaint/{id}")
    public List<Feedback> feedbackForComplaint(@PathVariable Long id){
        return feedbackService.findByGrievance(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_OFFICER')")
    public List<Feedback> allFeedback(){
        return feedbackService.findAll();
    }
}
