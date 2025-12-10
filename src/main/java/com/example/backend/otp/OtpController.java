package com.example.backend.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

import com.example.backend.model.Role;
import com.example.backend.service.UserService;

@RestController
@RequestMapping("/api/simple")
public class OtpController {

    private final OtpUserRepository repository;
    private final EmailService emailService;
    private final UserService userService;
    private final Random random = new Random();
    private static final Logger log = LoggerFactory.getLogger(OtpController.class);

    public OtpController(OtpUserRepository repository, EmailService emailService, UserService userService) {
        this.repository = repository;
        this.emailService = emailService;
        this.userService = userService;
    }

    private String generateOtp(){
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private String sendAndSave(OtpUser user){
        String otp = generateOtp();
        user.setOtp(otp);
        user.setExpiryTime(LocalDateTime.now().plusMinutes(10));
        repository.save(user);
        try{
            emailService.sendOtp(user.getEmail(), otp);
        }catch(RuntimeException ex){
            log.warn("Failed to send OTP email to {}. Returning OTP in response for debugging.", user.getEmail(), ex);
        }
        return otp;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody OtpUser payload){
        OtpUser user = repository.findByEmail(payload.getEmail()).orElse(new OtpUser());
        user.setFullname(payload.getFullname());
        user.setUsername(payload.getUsername());
        user.setEmail(payload.getEmail());
        user.setPassword(payload.getPassword());
        String otp = sendAndSave(user);
        return ResponseEntity.ok(Map.of(
            "message", "OTP generated",
            "otp", otp,
            "email", user.getEmail()
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body){
        String email = body.get("email");
        String otp = body.get("otp");
        return repository.findByEmail(email)
                .map(user -> {
                    if(user.getExpiryTime().isBefore(LocalDateTime.now())){
                        return ResponseEntity.badRequest().body(Map.of("error", "OTP expired"));
                    }
                    if(!user.getOtp().equals(otp)){
                        return ResponseEntity.badRequest().body(Map.of("error", "Invalid OTP"));
                    }
                    var registered = userService.registerUser(
                            user.getEmail(),
                            user.getUsername(),
                            user.getPassword(),
                            user.getFullname(),
                            Role.ROLE_CITIZEN
                    );
                    repository.delete(user);
                    return ResponseEntity.ok(Map.of(
                            "message", "Registration successful",
                            "username", registered.getUsername()
                    ));
                })
                .orElseGet(() -> ResponseEntity.badRequest().body(Map.of("error", "Invalid OTP")));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resend(@RequestBody Map<String, String> body){
        String email = body.get("email");
        return repository.findByEmail(email)
                .map(user -> {
                    String otp = sendAndSave(user);
                    return ResponseEntity.ok(Map.of("message", "OTP resent", "otp", otp));
                })
                .orElseGet(() -> ResponseEntity.badRequest().body(Map.of("error", "Email not found")));
    }
}
