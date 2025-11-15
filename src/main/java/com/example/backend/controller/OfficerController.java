package com.example.backend.controller;

import com.example.backend.dto.OfficerRequest;
import com.example.backend.model.Role;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/officers")
public class OfficerController {

    private final UserService userService;
    private final UserRepository userRepository;

    public OfficerController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    // Admin-only: add officer
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> addOfficer(@RequestBody OfficerRequest req){
        // create user with ROLE_OFFICER
        User u = userService.registerUser(req.getEmail(), req.getEmail().split("@")[0], req.getPassword(), req.getName(), Role.ROLE_OFFICER);
        // store phone in fullname? There's no dedicated phone on User model; optionally extend later.
        return ResponseEntity.ok(Map.of("id", u.getId(), "username", u.getUsername(), "email", u.getEmail()));
    }

    // Admin-only: list officers for dropdown
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> listOfficers(){
        var all = userRepository.findAll();
        var officers = all.stream()
                .filter(u -> u.getRoles() != null && u.getRoles().contains(Role.ROLE_OFFICER))
                .map(u -> Map.of("id", u.getId(), "name", u.getFullname() != null && !u.getFullname().isBlank() ? u.getFullname() : u.getUsername(), "email", u.getEmail()))
                .toList();
        return ResponseEntity.ok(officers);
    }
}
