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
import java.util.Optional;

/**
 * Manager for McpPlusSession lifecycle and storage.
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
public interface McpPlusSessionManager {
    
    /**
     * Create a new session.
     * 
     * @return the new session
     */
    McpPlusSession createSession();
    
    /**
     * Create a new session with specified max inactive interval.
     * 
     * @param maxInactiveInterval the maximum inactive interval
     * @return the new session
     */
    McpPlusSession createSession(Duration maxInactiveInterval);
    
    /**
     * Get a session by ID.
     * 
     * @param sessionId the session ID
     * @return the session if found and not expired
     */
    Optional<McpPlusSession> getSession(String sessionId);
    
    /**
     * Save or update a session.
     * 
     * @param session the session to save
     */
    void saveSession(McpPlusSession session);
    
    /**
     * Remove a session.
     * 
     * @param sessionId the session ID to remove
     */
    void removeSession(String sessionId);
    
    /**
     * Clean up expired sessions.
     * This method should be called periodically to remove expired sessions.
     * 
     * @return the number of sessions removed
     */
    int cleanupExpiredSessions();
    
    /**
     * Get the total number of active sessions.
     * 
     * @return the number of active sessions
     */
    int getActiveSessionCount();
}

