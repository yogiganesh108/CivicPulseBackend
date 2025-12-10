package com.example.backend.otp;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpUserRepository extends JpaRepository<OtpUser, Long> {
    Optional<OtpUser> findByEmail(String email);
}
