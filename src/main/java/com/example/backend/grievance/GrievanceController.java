package com.example.backend.grievance;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/grievances")
public class GrievanceController {

    private final GrievanceService service;
    private final UserRepository userRepository;

    public GrievanceController(GrievanceService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> submitGrievance(@RequestParam(required = false) String title,
                                             @RequestParam String description,
                                             @RequestParam String category,
                                             @RequestParam(required = false) String location,
                                             @RequestParam(required = false) MultipartFile image,
                                             Authentication authentication) throws IOException {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        Grievance g = new Grievance();
        g.setTitle(StringUtils.hasText(title) ? title : "");
        g.setDescription(description);
        g.setCategory(category);
        g.setLocation(location);
        g.setUserId(user.getId());
        g.setStatus(Status.PENDING);

        if (image != null && !image.isEmpty()) {
            // store image bytes in DB (BLOB)
            byte[] bytes = image.getBytes();
            g.setImageData(bytes);
            g.setImageType(image.getContentType());
        }

        Grievance saved = service.save(g);
        String imageUrl = null;
        if(saved.getImageData() != null){
            imageUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/api/grievances/")
                            .path(String.valueOf(saved.getId()))
                            .path("/image")
                            .toUriString();
        }
    return ResponseEntity.ok(Map.of(
        "id", saved.getId(),
        "title", saved.getTitle(),
        "description", saved.getDescription(),
        "category", saved.getCategory(),
        "location", saved.getLocation(),
        "imageUrl", imageUrl,
        "status", saved.getStatus(),
        "createdAt", saved.getCreatedAt()
    ));
    }

    @GetMapping("/me")
    public List<Map<String,Object>> myGrievances(Authentication authentication){
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    List<Grievance> list = service.findByUser(user.getId());
        return list.stream().map(g -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id", g.getId());
            m.put("title", g.getTitle());
            m.put("description", g.getDescription());
            m.put("category", g.getCategory());
            m.put("location", g.getLocation());
            if(g.getImageData() != null){
                String url = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/grievances/")
                        .path(String.valueOf(g.getId()))
                        .path("/image")
                        .toUriString();
                m.put("imageUrl", url);
            } else {
                m.put("imageUrl", null);
            }
            m.put("status", g.getStatus());
            m.put("createdAt", g.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
    }

    @GetMapping(path = "{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id){
        Optional<Grievance> maybe = service.findById(id);
        if(maybe.isEmpty()) return ResponseEntity.notFound().build();
        Grievance g = maybe.get();
        if(g.getImageData() == null || g.getImageData().length == 0) return ResponseEntity.notFound().build();
        String contentType = (g.getImageType() != null) ? g.getImageType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(g.getImageData());
    }

    @GetMapping(path = "{id}/resolution/image")
    public ResponseEntity<byte[]> getResolutionImage(@PathVariable Long id){
        Optional<Grievance> maybe = service.findById(id);
        if(maybe.isEmpty()) return ResponseEntity.notFound().build();
        Grievance g = maybe.get();
        if(g.getResolutionImageData() == null || g.getResolutionImageData().length == 0) return ResponseEntity.notFound().build();
        String contentType = (g.getResolutionImageType() != null) ? g.getResolutionImageType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(g.getResolutionImageData());
    }
}
