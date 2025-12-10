package com.example.backend.grievance;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
// import java.io.IOException;
// import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/grievances")
public class GrievanceController {

    private final GrievanceService service;
    private final UserRepository userRepository;
    private final com.example.backend.grievance.FeedbackService feedbackService;

    public GrievanceController(GrievanceService service, UserRepository userRepository, com.example.backend.grievance.FeedbackService feedbackService) {
        this.service = service;
        this.userRepository = userRepository;
        this.feedbackService = feedbackService;
    }

    @PutMapping(path = "{id}/image", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateMainImage(@PathVariable Long id,
                                             @RequestParam(required = false) org.springframework.web.multipart.MultipartFile image,
                                             Authentication authentication) throws IOException {
        try{
            var maybe = service.findById(id);
            if(maybe.isEmpty()) return ResponseEntity.notFound().build();
            Grievance g = maybe.get();

            // allow owner or admin to update main image
            String username = authentication.getName();
            User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
            boolean isOwner = user.getId() != null && user.getId().equals(g.getUserId());
            boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if(!isOwner && !isAdmin){
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not allowed"));
            }

            if(image != null && !image.isEmpty()){
                byte[] bytes = image.getBytes();
                g.setImageData(bytes);
                g.setImageType(image.getContentType());
                service.save(g);
            }
            return ResponseEntity.ok(Map.of("message", "updated", "id", g.getId()));
        }catch(Exception ex){
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> submitGrievance(@RequestParam(required = false) String title,
                                             @RequestParam String description,
                                             @RequestParam String category,
                                             @RequestParam(required = false) String subcategory,
                                             @RequestParam(required = false) String location,
                                             @RequestParam(required = false) MultipartFile image,
                                             Authentication authentication) throws IOException {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        Grievance g = new Grievance();
        g.setTitle(StringUtils.hasText(title) ? title : "");
        g.setDescription(description);
        g.setCategory(category);
        g.setSubcategory(subcategory);
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
        "subcategory", saved.getSubcategory(),
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
            m.put("subcategory", g.getSubcategory());
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
            // include assignment details for user visibility
            m.put("officerId", g.getOfficerId());
            m.put("priority", g.getPriority());
            m.put("deadline", g.getDeadline());
            // include resolution image if available
            if(g.getResolutionImageData() != null){
                String rurl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/grievances/")
                        .path(String.valueOf(g.getId()))
                        .path("/resolution/image")
                        .toUriString();
                m.put("resolutionImageUrl", rurl);
            }
            // include aggregated rating info
            Double avg = feedbackService.getAverageRatingForGrievance(g.getId());
            long count = feedbackService.getRatingCountForGrievance(g.getId());
            m.put("averageRating", avg);
            m.put("ratingCount", count);
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

    @GetMapping(path = "{id}/reopen/image")
    public ResponseEntity<byte[]> getReopenImage(@PathVariable Long id){
        Optional<Grievance> maybe = service.findById(id);
        if(maybe.isEmpty()) return ResponseEntity.notFound().build();
        Grievance g = maybe.get();
        if(g.getReopenImageData() == null || g.getReopenImageData().length == 0) return ResponseEntity.notFound().build();
        String contentType = (g.getReopenImageType() != null) ? g.getReopenImageType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(g.getReopenImageData());
    }

    @PutMapping(path = "{id}/reopen", consumes = {"multipart/form-data"})
    public ResponseEntity<?> submitReopenEvidence(@PathVariable Long id,
                                                 @RequestParam(required = false) org.springframework.web.multipart.MultipartFile image,
                                                 @RequestParam(required = false) String note,
                                                 Authentication authentication) {
        try{
            var maybe = service.findById(id);
            if(maybe.isEmpty()) return ResponseEntity.notFound().build();
            Grievance g = maybe.get();

            // only owner (citizen) may submit reopen evidence
            String username = authentication.getName();
            User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
            boolean isOwner = user.getId() != null && user.getId().equals(g.getUserId());
            if(!isOwner){
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not allowed"));
            }

            if(image != null && !image.isEmpty()){
                byte[] bytes = image.getBytes();
                g.setReopenImageData(bytes);
                g.setReopenImageType(image.getContentType());
            }
            if(note != null) g.setReopenNote(note);
            service.save(g);
            return ResponseEntity.ok(Map.of("message","reopen evidence saved"));
        }catch(Exception ex){
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportAll(Authentication authentication) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if(!isAdmin){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admins only"));
        }

        List<Grievance> grievances = service.findAll();
        byte[] bytes = buildWorkbook(grievances);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=grievances.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    private byte[] buildWorkbook(List<Grievance> grievances) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Complaints");
            String[] headers = {"ID", "Title", "Category", "Subcategory", "Status", "Priority", "Location", "OfficerId", "UserId", "Deadline", "CreatedAt"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (Grievance g : grievances) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(toText(g.getId()));
                row.createCell(1).setCellValue(toText(g.getTitle()));
                row.createCell(2).setCellValue(toText(g.getCategory()));
                row.createCell(3).setCellValue(toText(g.getSubcategory()));
                row.createCell(4).setCellValue(g.getStatus() != null ? g.getStatus().name() : "");
                row.createCell(5).setCellValue(toText(g.getPriority()));
                row.createCell(6).setCellValue(toText(g.getLocation()));
                row.createCell(7).setCellValue(toText(g.getOfficerId()));
                row.createCell(8).setCellValue(toText(g.getUserId()));
                row.createCell(9).setCellValue(g.getDeadline() != null ? g.getDeadline().toString() : "");
                row.createCell(10).setCellValue(g.getCreatedAt() != null ? g.getCreatedAt().toString() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate export", ex);
        }
    }

    private String toText(Object value) {
        return value == null ? "" : value.toString();
    }
}
