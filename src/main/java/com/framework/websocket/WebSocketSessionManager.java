package com.framework.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionManager.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("WebSocket session connected: {}", session.getId());
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session.getId());
        log.info("WebSocket session disconnected: {}", session.getId());
    }

    /**
     * Broadcast a message to all connected sessions.
     */
    public void broadcast(String jsonMessage) {
        TextMessage message = new TextMessage(jsonMessage);
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (IOException e) {
                log.error("Failed to send WebSocket message to session {}", session.getId(), e);
            }
        });
    }

    /**
     * Send a message to a specific session.
     */
    public void sendToSession(String sessionId, String jsonMessage) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(jsonMessage));
            } catch (IOException e) {
                log.error("Failed to send WebSocket message to session {}", sessionId, e);
            }
        }
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }
}
