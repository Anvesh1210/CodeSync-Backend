package com.codesync.session.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@RequiredArgsConstructor
public class YjsWebSocketHandler extends BinaryWebSocketHandler implements MessageListener {

    private final RedisTemplate<String, byte[]> binaryRedisTemplate;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    
    private static final String YJS_REDIS_CHANNEL_PREFIX = "yjs:room:";

    // Map: RoomId -> Set of WebSocketSessions
    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    // Map: SessionId -> RoomId
    private final Map<String, String> sessionRooms = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Subscribe to all yjs room channels on Redis
        redisMessageListenerContainer.addMessageListener(this, new PatternTopic(YJS_REDIS_CHANNEL_PREFIX + "*"));
        log.info("YjsWebSocketHandler initialized. Subscribed to Redis topic: {}*", YJS_REDIS_CHANNEL_PREFIX);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = getRoomId(session);
        if (roomId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        roomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(session);
        sessionRooms.put(session.getId(), roomId);
        log.info("Yjs client connected. Session: {}, Room: {}", session.getId(), roomId);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String roomId = sessionRooms.get(session.getId());
        if (roomId == null) return;

        ByteBuffer payload = message.getPayload();
        byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);

        // Broadcast to local clients in the same room (except sender)
        broadcastLocally(roomId, bytes, session.getId());

        // Publish to Redis so other instances can broadcast to their local clients
        binaryRedisTemplate.convertAndSend(YJS_REDIS_CHANNEL_PREFIX + roomId, bytes);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = sessionRooms.remove(session.getId());
        if (roomId != null) {
            Set<WebSocketSession> sessions = roomSessions.get(roomId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    roomSessions.remove(roomId);
                }
            }
            log.info("Yjs client disconnected. Session: {}, Room: {}", session.getId(), roomId);
        }
    }

    /**
     * Extracts room ID from the WebSocket URI path.
     * Expects path like: /yjs/{roomId}
     */
    private String getRoomId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return null;
    }

    /**
     * Broadcast a binary message to all clients in the given room on THIS instance.
     */
    private void broadcastLocally(String roomId, byte[] payload, String excludeSessionId) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) return;

        BinaryMessage message = new BinaryMessage(payload);
        for (WebSocketSession s : sessions) {
            if (s.isOpen() && (excludeSessionId == null || !s.getId().equals(excludeSessionId))) {
                try {
                    synchronized (s) {
                        s.sendMessage(message);
                    }
                } catch (IOException e) {
                    log.error("Failed to send Yjs message to session {}", s.getId(), e);
                }
            }
        }
    }

    /**
     * Receives messages from Redis Pub/Sub (published by other instances).
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        if (!channel.startsWith(YJS_REDIS_CHANNEL_PREFIX)) return;

        String roomId = channel.substring(YJS_REDIS_CHANNEL_PREFIX.length());
        byte[] payload = message.getBody();

        // Broadcast to all local clients in this room (null exclude = broadcast to all)
        broadcastLocally(roomId, payload, null);
    }
}
