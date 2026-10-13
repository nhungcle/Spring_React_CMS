package com.framework.controller;

import com.framework.dto.AuthRequest;
import com.framework.dto.AuthResponse;
import com.framework.model.Role;
import com.framework.model.User;
import com.framework.repository.RoleRepository;
import com.framework.repository.UserRepository;
import com.framework.security.JwtTokenProvider;
import com.framework.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider,
                          UserRepository userRepository, RoleRepository roleRepository) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * POST /auth — Login.
     * Frontend sends { username: email, password }.
     * Returns { user: { first_name, last_name, user_name, email, roles } }
     * and sets HttpOnly cookie with JWT.
     */
    @PostMapping
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        User user = authService.authenticate(request.getUsername(), request.getPassword());
        String token = jwtTokenProvider.generateToken(user);

        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .path("/")
                .maxAge(24 * 60 * 60) // 24 hours
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(user));
    }

    /**
     * GET /auth — Validate token and return current user.
     * The JwtAuthenticationFilter already validated the token.
     */
    @GetMapping
    public ResponseEntity<User> validateToken(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        User user = authService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    /**
     * POST /auth/logout — Clear the token cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    /**
     * POST /auth/permission — Check if user has requested roles.
     * Frontend sends { nodes: ["Admin", "User"] }.
     * Returns { "Admin": true, "User": false }.
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/permission")
    public ResponseEntity<Map<String, Boolean>> checkPermission(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {

        String email = (String) request.getAttribute("userEmail");
        User user = authService.getUserByEmail(email);
        Set<String> userRoles = user.getRoleNames();

        List<String> nodes = (List<String>) body.get("nodes");
        Map<String, Boolean> result = new LinkedHashMap<>();

        if (nodes != null) {
            for (String node : nodes) {
                result.put(node, userRoles.contains(node));
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * POST /auth/register — Public user registration.
     * Creates a new user with the default "User" role.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> body) {
        String userName = body.get("userName");
        String firstName = body.get("firstName");
        String lastName = body.get("lastName");
        String email = body.get("email");
        String password = body.get("password");
        String passwordConfirm = body.get("passwordConfirm");

        // Validate required fields
        if (userName == null || userName.isBlank()) throw new IllegalArgumentException("Username is required");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required");
        if (!password.equals(passwordConfirm)) throw new IllegalArgumentException("Passwords do not match");

        // Check uniqueness
        if (userRepository.findByEmail(email.trim()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (userRepository.findByUserName(userName.trim()).isPresent()) {
            throw new IllegalArgumentException("Username already in use");
        }

        // Create user with hashed password
        User user = new User();
        user.setUserName(userName.trim());
        user.setFirstName(firstName != null ? firstName.trim() : "");
        user.setLastName(lastName != null ? lastName.trim() : "");
        user.setEmail(email.trim());

        String salt = UUID.randomUUID().toString();
        user.setPasswordSalt(salt);
        user.setPassword(User.hashPassword(password, salt));

        user = userRepository.save(user);

        // Assign default "User" role
        Role userRole = roleRepository.findByName("User")
                .orElse(null);
        if (userRole != null) {
            user.getRoles().add(userRole);
            userRepository.save(user);
        }

        return ResponseEntity.ok(Map.of("status", "ok", "message", "Registration successful"));
    }
}
