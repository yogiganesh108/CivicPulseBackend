package com.example.backend.grievance;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GrievanceService {
    private final GrievanceRepository repo;

    public GrievanceService(GrievanceRepository repo) { this.repo = repo; }

    public Grievance save(Grievance g){ return repo.save(g); }

    public List<Grievance> findByUser(Long userId){ return repo.findByUserIdOrderByCreatedAtDesc(userId); }

    public Optional<Grievance> findById(Long id){ return repo.findById(id); }

    public List<Grievance> findAll(){ return repo.findAll(); }

    public Grievance assignOfficer(Long grievanceId, Long officerId, String priority, java.time.LocalDate deadline){
        Grievance g = repo.findById(grievanceId).orElseThrow(() -> new RuntimeException("Grievance not found"));
        g.setOfficerId(officerId);
        g.setPriority(priority);
        g.setDeadline(deadline);
        g.setStatus(Status.ASSIGNED);
        return repo.save(g);
    }

    public List<Grievance> findByOfficer(Long officerId){
        return repo.findByOfficerIdOrderByCreatedAtDesc(officerId);
    }

    public Grievance updateResolution(Long id, String statusStr, String resolutionNote, byte[] imageData, String imageType){
        Grievance g = repo.findById(id).orElseThrow(() -> new RuntimeException("Grievance not found"));
        if(statusStr != null){
            try{
                Status s = Status.valueOf(statusStr);
                g.setStatus(s);
            }catch(Exception ex){ /* ignore invalid status */ }
        }
        if(resolutionNote != null) g.setResolutionNote(resolutionNote);
        if(imageData != null && imageData.length > 0){
            g.setResolutionImageData(imageData);
        }
        if("RESOLVED".equalsIgnoreCase(statusStr)){
            g.setResolvedAt(java.time.Instant.now());
        }
        return repo.save(g);
    }
}
