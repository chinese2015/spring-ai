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

package org.springframework.ai.mcp.plus.core.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a session in MCP Plus context, providing session-scoped data storage
 * and lifecycle management.
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
public interface McpPlusSession {
    
    /**
     * Get the unique session identifier.
     * 
     * @return the session ID
     */
    String getId();
    
    /**
     * Get the time when this session was created.
     * 
     * @return the creation timestamp
     */
    Instant getCreatedAt();
    
    /**
     * Get the time when this session was last accessed.
     * 
     * @return the last access timestamp
     */
    Instant getLastAccessedAt();
    
    /**
     * Get the maximum inactive interval for this session.
     * 
     * @return the maximum inactive interval
     */
    Duration getMaxInactiveInterval();
    
    /**
     * Check if this session has expired.
     * 
     * @return true if the session has expired
     */
    boolean isExpired();
    
    /**
     * Get typed data from the session.
     * 
     * @param <T> the expected type of the data
     * @param key the data key
     * @param type the expected class of the data
     * @return the data value if present and of the correct type
     */
    <T> Optional<T> getData(String key, Class<T> type);
    
    /**
     * Set data in the session.
     * 
     * @param key the data key
     * @param value the data value
     */
    void setData(String key, Object value);
    
    /**
     * Remove data from the session.
     * 
     * @param key the data key
     */
    void removeData(String key);
    
    /**
     * Get all session data as an immutable map.
     * 
     * @return all session data
     */
    Map<String, Object> getAllData();
    
    /**
     * Invalidate this session, clearing all data and marking it as expired.
     */
    void invalidate();
    
    /**
     * Touch the session to update the last accessed time.
     */
    void touch();
    
    /**
     * Get the underlying MCP server session if available.
     * This provides access to the low-level MCP Java SDK McpServerSession object.
     * 
     * @return the underlying MCP server session if present
     */
    Optional<Object> getUnderlyingMcpServerSession();
}

