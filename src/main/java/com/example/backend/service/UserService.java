package com.example.backend.service;

import com.example.backend.model.Role;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String email, String username, String rawPassword, String fullname, Role role) {
        if (userRepository.existsByUsername(username)) throw new RuntimeException("Username already taken");
        if (userRepository.existsByEmail(email)) throw new RuntimeException("Email already taken");
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setFullname(fullname);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRoles(new HashSet<>(Collections.singletonList(role)));
        return userRepository.save(u);
    }
}
