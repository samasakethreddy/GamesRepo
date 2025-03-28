package com.Games.deployment.controller;

import com.Games.deployment.config.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public final AuthenticationManager authenticationManager;
    public final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

//    @PostMapping("/login")
//    public ResponseEntity<Map<String, String>> login(
//            @RequestParam String username, @RequestParam String password
//    ) {
//        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
//        String token = jwtUtil.generateToken(username);
//
//        System.out.println("Generated Token: " + token); // Debugging Line
//
//        Map<String, String> response = new HashMap<>();
//        response.put("token", token);
//
//        return ResponseEntity.ok(response);
//    }


    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(HttpServletRequest request) {
        // Extract credentials from Basic Auth Header
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing Basic Authentication Header"));
        }

        // Decode Basic Auth (Username:Password)
        String base64Credentials = authHeader.substring("Basic ".length());
        byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Credentials);
        String credentials = new String(decodedBytes);
        String[] userPass = credentials.split(":", 2);

        if (userPass.length != 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid Basic Authentication Header"));
        }

        String username = userPass[0];
        String password = userPass[1];

        // Authenticate
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

        // Generate JWT Token
        String token = jwtUtil.generateToken(username);

        // Response
        Map<String, String> response = new HashMap<>();
        response.put("token", token);

        return ResponseEntity.ok(response);
    }


    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestHeader("Authorization") String token) {
        String newToken = jwtUtil.refreshJwtToken(token.replace("Bearer ", ""));
        Map<String, String> response = new HashMap<>();
        response.put("token", newToken);
        return ResponseEntity.ok(response);
    }

}
