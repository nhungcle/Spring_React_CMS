package com.framework.controller;

import com.framework.model.ContactSubmission;
import com.framework.repository.ContactSubmissionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cms/contact")
public class ContactController {

    private final ContactSubmissionRepository repository;

    public ContactController(ContactSubmissionRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, String>> submit(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String email = body.get("email");
        String phone = body.get("phone");
        String message = body.get("message");

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        ContactSubmission sub = new ContactSubmission();
        sub.setName(name.trim());
        sub.setEmail(email.trim());
        sub.setPhone(phone != null ? phone.trim() : null);
        sub.setMessage(message != null ? message.trim() : null);
        sub.setStatus("new");
        sub.setSubmittedAt(Instant.now());
        sub.setCreatedAt(Instant.now());
        sub.setUpdatedAt(Instant.now());

        repository.save(sub);
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Submission received"));
    }

    @GetMapping("/submissions")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<List<ContactSubmission>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PutMapping("/submissions/{id}/read")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ContactSubmission> markAsRead(@PathVariable Long id) {
        ContactSubmission sub = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + id));
        sub.setStatus("read");
        sub.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(repository.save(sub));
    }

    @DeleteMapping("/submissions/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
