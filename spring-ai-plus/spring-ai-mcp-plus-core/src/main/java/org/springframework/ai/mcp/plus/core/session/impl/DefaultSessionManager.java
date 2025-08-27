/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.mcp.plus.core.session.impl;

import org.springframework.ai.mcp.plus.core.session.McpPlusSession;
import org.springframework.ai.mcp.plus.core.session.McpPlusSessionManager;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Default in-memory implementation of McpPlusSessionManager.
 * Uses ConcurrentHashMap for thread-safe session storage.
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
public class DefaultSessionManager implements McpPlusSessionManager {
    
    private final Map<String, McpPlusSession> sessions = new ConcurrentHashMap<>();
    private final Duration defaultMaxInactiveInterval;
    private final ScheduledExecutorService cleanupExecutor;
    private final boolean autoCleanup;
    
    /**
     * Create a session manager with default settings.
     */
    public DefaultSessionManager() {
        this(Duration.ofMinutes(30), true);
    }
    
    /**
     * Create a session manager with specified default max inactive interval.
     * 
     * @param defaultMaxInactiveInterval default max inactive interval for new sessions
     */
    public DefaultSessionManager(Duration defaultMaxInactiveInterval) {
        this(defaultMaxInactiveInterval, true);
    }
    
    /**
     * Create a session manager with full configuration.
     * 
     * @param defaultMaxInactiveInterval default max inactive interval for new sessions
     * @param autoCleanup whether to automatically clean up expired sessions
     */
    public DefaultSessionManager(Duration defaultMaxInactiveInterval, boolean autoCleanup) {
        this.defaultMaxInactiveInterval = defaultMaxInactiveInterval != null ? 
            defaultMaxInactiveInterval : Duration.ofMinutes(30);
        this.autoCleanup = autoCleanup;
        
        if (autoCleanup) {
            this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "McpPlusSessionCleanup");
                t.setDaemon(true);
                return t;
            });
            
            // Schedule cleanup every 5 minutes
            this.cleanupExecutor.scheduleWithFixedDelay(
                this::cleanupExpiredSessions, 
                5, 5, TimeUnit.MINUTES
            );
        } else {
            this.cleanupExecutor = null;
        }
    }
    
    @Override
    public McpPlusSession createSession() {
        return createSession(defaultMaxInactiveInterval);
    }
    
    @Override
    public McpPlusSession createSession(Duration maxInactiveInterval) {
        Duration interval = maxInactiveInterval != null ? maxInactiveInterval : defaultMaxInactiveInterval;
        McpPlusSession session = new DefaultMcpPlusSession(interval);
        sessions.put(session.getId(), session);
        return session;
    }
    
    @Override
    public Optional<McpPlusSession> getSession(String sessionId) {
        if (sessionId == null) {
            return Optional.empty();
        }
        
        McpPlusSession session = sessions.get(sessionId);
        if (session == null) {
            return Optional.empty();
        }
        
        if (session.isExpired()) {
            sessions.remove(sessionId);
            return Optional.empty();
        }
        
        // Touch the session to update last access time
        session.touch();
        return Optional.of(session);
    }
    
    @Override
    public void saveSession(McpPlusSession session) {
        if (session == null || session.getId() == null) {
            return;
        }
        
        if (!session.isExpired()) {
            sessions.put(session.getId(), session);
        }
    }
    
    @Override
    public void removeSession(String sessionId) {
        if (sessionId != null) {
            McpPlusSession session = sessions.remove(sessionId);
            if (session != null) {
                session.invalidate();
            }
        }
    }
    
    @Override
    public int cleanupExpiredSessions() {
        int removedCount = 0;
        
        for (Map.Entry<String, McpPlusSession> entry : sessions.entrySet()) {
            McpPlusSession session = entry.getValue();
            if (session.isExpired()) {
                sessions.remove(entry.getKey());
                session.invalidate();
                removedCount++;
            }
        }
        
        return removedCount;
    }
    
    @Override
    public int getActiveSessionCount() {
        // Remove expired sessions before counting
        cleanupExpiredSessions();
        return sessions.size();
    }
    
    /**
     * Shutdown the session manager and cleanup resources.
     */
    public void shutdown() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Invalidate all sessions
        for (McpPlusSession session : sessions.values()) {
            session.invalidate();
        }
        sessions.clear();
    }
    
    @Override
    public String toString() {
        return "DefaultSessionManager{" +
               "activeSessionCount=" + sessions.size() +
               ", defaultMaxInactiveInterval=" + defaultMaxInactiveInterval +
               ", autoCleanup=" + autoCleanup +
               '}';
    }
}

