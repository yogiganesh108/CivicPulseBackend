package com.example.backend.security;

import io.jsonwebtoken.Jwts;

import java.util.ArrayList;
import java.util.List;
// import java.util.Map;

public class JwtsParser {
    @SuppressWarnings("unchecked")
    public static List<String> parseRoles(String token) {
        var body = Jwts.parserBuilder().build().parseClaimsJws(token).getBody();
        Object obj = body.get("roles");
        if (obj instanceof List) {
            return (List<String>) obj;
        }
        return new ArrayList<>();
    }
}
