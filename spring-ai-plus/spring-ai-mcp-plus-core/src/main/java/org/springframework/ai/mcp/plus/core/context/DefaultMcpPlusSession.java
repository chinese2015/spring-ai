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

package org.springframework.ai.mcp.plus.core.context;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of McpPlusSession.
 * Thread-safe implementation using ConcurrentHashMap for session data storage.
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
public class DefaultMcpPlusSession implements McpPlusSession {
    
    private final String id;
    private final Instant createdAt;
    private volatile Instant lastAccessedAt;
    private final Duration maxInactiveInterval;
    private volatile boolean invalidated;
    private final Map<String, Object> sessionData;
    private final Object underlyingMcpSession;
    
    /**
     * Create a new session with default settings.
     */
    public DefaultMcpPlusSession() {
        this(UUID.randomUUID().toString(), Duration.ofMinutes(30), null);
    }
    
    /**
     * Create a new session with specified max inactive interval.
     * 
     * @param maxInactiveInterval maximum time a session can be inactive
     */
    public DefaultMcpPlusSession(Duration maxInactiveInterval) {
        this(UUID.randomUUID().toString(), maxInactiveInterval, null);
    }
    
    /**
     * Create a new session with specified ID and max inactive interval.
     * 
     * @param id the session ID
     * @param maxInactiveInterval maximum time a session can be inactive
     * @param underlyingMcpSession the underlying MCP session object, if any
     */
    public DefaultMcpPlusSession(String id, Duration maxInactiveInterval, Object underlyingMcpSession) {
        this.id = id;
        this.createdAt = Instant.now();
        this.lastAccessedAt = this.createdAt;
        this.maxInactiveInterval = maxInactiveInterval != null ? maxInactiveInterval : Duration.ofMinutes(30);
        this.invalidated = false;
        this.sessionData = new ConcurrentHashMap<>();
        this.underlyingMcpSession = underlyingMcpSession;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }
    
    @Override
    public Duration getMaxInactiveInterval() {
        return maxInactiveInterval;
    }
    
    @Override
    public boolean isExpired() {
        if (invalidated) {
            return true;
        }
        
        Instant now = Instant.now();
        return now.isAfter(lastAccessedAt.plus(maxInactiveInterval));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getData(String key, Class<T> type) {
        if (key == null || type == null) {
            return Optional.empty();
        }
        
        checkNotExpired();
        touch();
        
        Object value = sessionData.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        
        return Optional.empty();
    }
    
    @Override
    public void setData(String key, Object value) {
        if (key == null) {
            return;
        }
        
        checkNotExpired();
        touch();
        
        if (value == null) {
            sessionData.remove(key);
        } else {
            sessionData.put(key, value);
        }
    }
    
    @Override
    public void removeData(String key) {
        if (key == null) {
            return;
        }
        
        checkNotExpired();
        touch();
        
        sessionData.remove(key);
    }
    
    @Override
    public Map<String, Object> getAllData() {
        checkNotExpired();
        touch();
        
        return Collections.unmodifiableMap(sessionData);
    }
    
    @Override
    public void invalidate() {
        this.invalidated = true;
        this.sessionData.clear();
    }
    
    @Override
    public void touch() {
        if (!invalidated) {
            this.lastAccessedAt = Instant.now();
        }
    }
    
    @Override
    public Optional<Object> getUnderlyingMcpSession() {
        return Optional.ofNullable(underlyingMcpSession);
    }
    
    private void checkNotExpired() {
        if (isExpired()) {
            throw new IllegalStateException("Session has expired: " + id);
        }
    }
    
    @Override
    public String toString() {
        return "DefaultMcpPlusSession{" +
               "id='" + id + '\'' +
               ", createdAt=" + createdAt +
               ", lastAccessedAt=" + lastAccessedAt +
               ", maxInactiveInterval=" + maxInactiveInterval +
               ", invalidated=" + invalidated +
               ", dataSize=" + sessionData.size() +
               '}';
    }
}
