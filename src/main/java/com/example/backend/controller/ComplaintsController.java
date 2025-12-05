package com.example.backend.controller;

import com.example.backend.grievance.Grievance;
import com.example.backend.grievance.GrievanceService;
import com.example.backend.dto.AssignRequest;
import com.example.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintsController {

    private final GrievanceService grievanceService;
    private final UserRepository userRepository;
    private final com.example.backend.grievance.FeedbackService feedbackService;

    public ComplaintsController(GrievanceService grievanceService, UserRepository userRepository, com.example.backend.grievance.FeedbackService feedbackService) {
        this.grievanceService = grievanceService;
        this.userRepository = userRepository;
        this.feedbackService = feedbackService;
    }

    // Get single complaint by id (admin/officer/user may call depending on auth)
    @GetMapping("/{id}")
    public ResponseEntity<?> getComplaint(@PathVariable Long id){
        try{
            Grievance g = grievanceService.findById(id).orElseThrow(() -> new RuntimeException("Grievance not found"));
            Map<String,Object> m = new HashMap<>();
            m.put("id", g.getId());
            m.put("title", g.getTitle());
            m.put("description", g.getDescription());
            m.put("category", g.getCategory());
            m.put("location", g.getLocation());
            if(g.getImageData() != null){
                String imageUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/grievances/")
                        .path(String.valueOf(g.getId()))
                        .path("/image")
                        .toUriString();
                m.put("imageUrl", imageUrl);
            } else {
                m.put("imageUrl", null);
            }
            m.put("status", g.getStatus() != null ? g.getStatus().name() : null);
            m.put("userId", g.getUserId());
            m.put("officerId", g.getOfficerId());
            m.put("priority", g.getPriority());
            m.put("deadline", g.getDeadline());
            m.put("createdAt", g.getCreatedAt());
            m.put("resolutionNote", g.getResolutionNote());
            if(g.getResolutionImageData() != null){
                String rurl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/grievances/")
                        .path(String.valueOf(g.getId()))
                        .path("/resolution/image")
                        .toUriString();
                m.put("resolutionImageUrl", rurl);
            }
            if(g.getReopenImageData() != null){
                String reopenUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/grievances/")
                        .path(String.valueOf(g.getId()))
                        .path("/reopen/image")
                        .toUriString();
                m.put("reopenImageUrl", reopenUrl);
            }
            if(g.getReopenNote() != null) m.put("reopenNote", g.getReopenNote());
            Double avg = feedbackService.getAverageRatingForGrievance(g.getId());
            long count = feedbackService.getRatingCountForGrievance(g.getId());
            m.put("averageRating", avg);
            m.put("ratingCount", count);
            return ResponseEntity.ok(m);
        }catch(Exception ex){
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // Admin: list all complaints (return mapped DTOs so frontend can consume imageUrl and other derived fields)
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<Map<String,Object>> listAll(){
        return grievanceService.findAll().stream().map(g -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id", g.getId());
            m.put("title", g.getTitle());
            m.put("description", g.getDescription());
            m.put("category", g.getCategory());
            m.put("location", g.getLocation());
            String imageUrl = null;
            if(g.getImageData() != null){
                imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/grievances/")
                        .path(String.valueOf(g.getId()))
                        .path("/image")
                        .toUriString();
            }
            m.put("imageUrl", imageUrl);
            m.put("status", g.getStatus() != null ? g.getStatus().name() : null);
            m.put("userId", g.getUserId());
            m.put("officerId", g.getOfficerId());
            m.put("priority", g.getPriority());
            m.put("deadline", g.getDeadline());
            m.put("createdAt", g.getCreatedAt());
            Double avg = feedbackService.getAverageRatingForGrievance(g.getId());
            long count = feedbackService.getRatingCountForGrievance(g.getId());
            m.put("averageRating", avg);
            m.put("ratingCount", count);
            if(g.getReopenImageData() != null){
                String reopenUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/grievances/")
                        .path(String.valueOf(g.getId()))
                        .path("/reopen/image")
                        .toUriString();
                m.put("reopenImageUrl", reopenUrl);
            }
            if(g.getReopenNote() != null) m.put("reopenNote", g.getReopenNote());
            return m;
        }).toList();
    }

    // Admin: assign officer, set priority & deadline
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> assignOfficer(@PathVariable Long id, @RequestBody AssignRequest req){
        try{
            Grievance updated = grievanceService.assignOfficer(id, req.getOfficer_id(), req.getPriority(), req.getDeadline());
            // TODO: send notification/email to officer — implement integration later
            return ResponseEntity.ok(Map.of("message", "assigned", "id", updated.getId()));
        }catch(Exception ex){
            // Return a helpful error to the client instead of 500 stack trace
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // User: fetch complaints by user id (admin may also use)
    @GetMapping("/user/{userId}")
    public List<Grievance> byUser(@PathVariable Long userId){
        return grievanceService.findByUser(userId);
    }

    // Officer: list complaints assigned to me
    @GetMapping("/officer/me")
    public List<Map<String,Object>> myAssigned(org.springframework.security.core.Authentication authentication){
        String username = authentication.getName();
        var user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        return grievanceService.findByOfficer(user.getId()).stream().map(g -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id", g.getId());
            m.put("title", g.getTitle());
            m.put("description", g.getDescription());
            m.put("category", g.getCategory());
            m.put("location", g.getLocation());
            String imageUrl = null;
            if(g.getImageData() != null){
                imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/grievances/")
                        .path(String.valueOf(g.getId()))
                        .path("/image")
                        .toUriString();
            }
            m.put("imageUrl", imageUrl);
            m.put("status", g.getStatus() != null ? g.getStatus().name() : null);
            m.put("priority", g.getPriority());
            m.put("deadline", g.getDeadline());
            m.put("createdAt", g.getCreatedAt());
            m.put("resolutionNote", g.getResolutionNote());
            Double avg = feedbackService.getAverageRatingForGrievance(g.getId());
            long count = feedbackService.getRatingCountForGrievance(g.getId());
            m.put("averageRating", avg);
            m.put("ratingCount", count);
            if(g.getResolutionImageData() != null){
                // Serve resolution images from the grievances controller (these paths are permitted by security config)
                String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/grievances/")
                        .path(String.valueOf(g.getId()))
                        .path("/resolution/image")
                        .toUriString();
                m.put("resolutionImageUrl", url);
            }
            return m;
        }).toList();
    }

    // Admin: list complaints assigned to a specific officer
    @GetMapping("/officer/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<Map<String,Object>> byOfficer(@PathVariable Long id){
        return grievanceService.findByOfficer(id).stream().map(g -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id", g.getId());
            m.put("title", g.getTitle());
            m.put("description", g.getDescription());
            m.put("category", g.getCategory());
            m.put("location", g.getLocation());
            String imageUrl = null;
            if(g.getImageData() != null){
                imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/grievances/")
                        .path(String.valueOf(g.getId()))
                        .path("/image")
                        .toUriString();
            }
            m.put("imageUrl", imageUrl);
            m.put("status", g.getStatus() != null ? g.getStatus().name() : null);
            m.put("userId", g.getUserId());
            m.put("priority", g.getPriority());
            m.put("deadline", g.getDeadline());
            m.put("createdAt", g.getCreatedAt());
            m.put("resolutionNote", g.getResolutionNote());
            Double avg = feedbackService.getAverageRatingForGrievance(g.getId());
            long count = feedbackService.getRatingCountForGrievance(g.getId());
            m.put("averageRating", avg);
            m.put("ratingCount", count);
            return m;
        }).toList();
    }

    // Update resolution/status (multipart)
    @PutMapping(path = "/{id}/update", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateResolutionMultipart(@PathVariable Long id,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String resolutionNote,
                                                       @RequestParam(required = false) org.springframework.web.multipart.MultipartFile resolutionImage) {
        try{
            byte[] data = null;
            String imageType = null;
            if(resolutionImage != null && !resolutionImage.isEmpty()){
                data = resolutionImage.getBytes();
                imageType = resolutionImage.getContentType();
            }
            Grievance updated = grievanceService.updateResolution(id, status, resolutionNote, data, imageType);
            return ResponseEntity.ok(Map.of("message", "updated", "id", updated.getId()));
        }catch(Exception ex){
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // JSON-only update endpoint
    @PutMapping(path = "/{id}/update", consumes = {"application/json"}, produces = "application/json")
    public ResponseEntity<?> updateResolutionJson(@PathVariable Long id, @RequestBody Map<String, Object> body){
        try{
            String status = body.containsKey("status") ? (String) body.get("status") : null;
            String note = body.containsKey("resolutionNote") ? (String) body.get("resolutionNote") : null;
            Grievance updated = grievanceService.updateResolution(id, status, note, null, null);
            return ResponseEntity.ok(Map.of("message", "updated", "id", updated.getId()));
        }catch(Exception ex){
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
