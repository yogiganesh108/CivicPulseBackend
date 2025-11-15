package com.example.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @GetMapping("/echo")
    public Map<String, Object> echoGet(@RequestHeader Map<String, String> headers) {
        return Map.of(
                "method", "GET",
                "headers", headers
        );
    }

    @GetMapping("/whoami")
    public Map<String,Object> whoami(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null) return Map.of("authenticated", false);
        var roles = auth.getAuthorities().stream().map(a->a.getAuthority()).collect(Collectors.toList());
        return Map.of(
                "authenticated", true,
                "principal", auth.getPrincipal(),
                "authorities", roles
        );
    }

    @PostMapping("/echo")
    public Map<String, Object> echoPost(@RequestHeader Map<String, String> headers) {
        return Map.of(
                "method", "POST",
                "headers", headers
        );
    }
}
