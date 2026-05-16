package com.hud.briefing;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus(Authentication authentication) {
        Map<String, Object> status = new HashMap<>();
        if (authentication != null && authentication.isAuthenticated()) {
            status.put("authenticated", true);
            status.put("username", authentication.getName());
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            status.put("roles", roles);
            status.put("isAdmin", roles.contains("ROLE_ADMIN"));
            boolean mustChange = userRepository.findByUsername(authentication.getName())
                    .map(AppUser::isPasswordChangeRequired)
                    .orElse(false);
            status.put("passwordChangeRequired", mustChange);
        } else {
            status.put("authenticated", false);
            status.put("isAdmin", false);
            status.put("passwordChangeRequired", false);
        }
        return status;
    }

    @PutMapping("/password")
    public Map<String, String> changePassword(@RequestBody Map<String, String> request,
                                              Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        String newPassword = request.get("newPassword");
        validateStrength(newPassword);

        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    private void validateStrength(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must contain at least one letter and one digit");
        }
    }
}
