package com.framework.dto;

import com.framework.model.User;

/**
 * Response wrapper for auth endpoints.
 * Serializes as { "user": { first_name, last_name, user_name, email, roles } }
 */
public class AuthResponse {

    private User user;

    public AuthResponse() {}

    public AuthResponse(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
