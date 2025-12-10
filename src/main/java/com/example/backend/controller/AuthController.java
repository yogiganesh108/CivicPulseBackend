package com.example.backend.controller;

import com.example.backend.dto.AuthRequest;
import com.example.backend.dto.RegistrationRequest;
// import com.example.backend.dto.AuthResponse;
import com.example.backend.model.Role;
import com.example.backend.model.User;
import com.example.backend.security.JwtUtils;
import com.example.backend.service.AppUserDetailsService;
import com.example.backend.service.UserService;
import com.example.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.Authentication; // not used
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final AppUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils, UserService userService, AppUserDetailsService userDetailsService, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String role, @RequestBody RegistrationRequest req) {
        // Basic input validation to provide clearer error messages
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email is required"));
        }
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username is required"));
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "password is required"));
        }
        if (req.getFullname() == null || req.getFullname().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "fullname is required"));
        }

        // Validate role parameter
        String rname = role == null ? "" : role.toLowerCase();
        if (!Set.of("citizen", "officer", "admin").contains(rname)) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid role, must be citizen|officer|admin"));
        }
        Role r;
        try {
            r = Role.valueOf("ROLE_" + rname.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid role"));
        }

        User u = userService.registerUser(req.getEmail(), req.getUsername(), req.getPassword(), req.getFullname(), r);
        // Return JSON so frontend can parse with res.json()
        return ResponseEntity.ok(Map.of("username", u.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        UserDetails ud = userDetailsService.loadUserByUsername(req.getUsername());
        Set<String> roles = ud.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet());
        String token = jwtUtils.generateToken(ud.getUsername(), roles);
        // fetch user id and primary role for frontend convenience
        com.example.backend.model.User u = userRepository.findByUsername(req.getUsername()).orElse(null);
        String primaryRole = roles.stream().findFirst().orElse(null);
        return ResponseEntity.ok(Map.of(
                "user_id", u != null ? u.getId() : null,
                "username", ud.getUsername(),
                "role", primaryRole,
                "token", token
        ));
    }
}
