package com.framework.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * Raw WebSocket handler (no STOMP).
 * The frontend uses native WebSocket with JSON messages: { cmd, data, success }.
 */
@Component
public class FrameworkWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(FrameworkWebSocketHandler.class);

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public FrameworkWebSocketHandler(WebSocketSessionManager sessionManager, ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionManager.addSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);

            String cmd = (String) payload.get("cmd");
            Object data = payload.get("data");

            // Process the message — override this method in subclasses for custom behavior
            Map<String, Object> response = processMessage(cmd, data, session);

            String responseJson = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(responseJson));

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            Map<String, Object> errorResponse = Map.of(
                    "cmd", "error",
                    "success", false,
                    "data", Map.of("message", e.getMessage())
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorResponse)));
        }
    }

    /**
     * Process an incoming WebSocket message.
     * Override in subclasses for application-specific behavior.
     */
    protected Map<String, Object> processMessage(String cmd, Object data, WebSocketSession session) {
        // Default echo behavior — applications should override this
        return Map.of(
                "cmd", cmd != null ? cmd : "echo",
                "success", true,
                "data", data != null ? data : Map.of()
        );
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionManager.removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}", session.getId(), exception);
        sessionManager.removeSession(session);
    }
}
