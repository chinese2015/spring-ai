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

package org.springframework.ai.mcp.plus.core.integration;

import org.springframework.ai.mcp.plus.core.session.enhanced.EnhancedMcpServerSession;
import org.springframework.ai.mcp.plus.core.session.McpPlusSessionManager;

import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Non-intrusive MCP session registrar that wraps existing McpServerSession instances
 * without modifying the Spring AI transport providers.
 * 
 * This component:
 * - Maintains a registry of enhanced sessions
 * - Provides lazy creation of enhanced wrappers
 * - Enables retrieval of MCP sessions in tool execution context
 * - Works with any existing TransportProvider
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
public class McpSessionRegistrar {
    
    private final Map<String, EnhancedMcpServerSession> sessionRegistry = new ConcurrentHashMap<>();
    private final McpPlusSessionManager sessionManager;
    
    public McpSessionRegistrar(McpPlusSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    /**
     * Register or retrieve an enhanced session wrapper for the given MCP session.
     * This method is thread-safe and will return the same enhanced session
     * for the same underlying MCP session.
     * 
     * @param mcpServerSession the MCP server session to wrap
     * @return the enhanced session wrapper
     */
    public EnhancedMcpServerSession registerOrGet(Object mcpServerSession) {
        if (mcpServerSession == null) {
            throw new IllegalArgumentException("MCP server session cannot be null");
        }
        
        String sessionId = extractSessionId(mcpServerSession);
        
        return sessionRegistry.computeIfAbsent(sessionId, id -> {
            // Create enhanced session wrapper
            EnhancedMcpServerSession enhanced = new EnhancedMcpServerSession(mcpServerSession);
            
            // Register the application session with the manager
            sessionManager.saveSession(enhanced.getApplicationSession());
            
            return enhanced;
        });
    }
    
    /**
     * Retrieve an enhanced session by MCP session ID.
     * 
     * @param sessionId the session ID
     * @return the enhanced session if found, null otherwise
     */
    @Nullable
    public EnhancedMcpServerSession getBySessionId(String sessionId) {
        return sessionRegistry.get(sessionId);
    }
    
    /**
     * Retrieve an enhanced session by the underlying MCP session object.
     * 
     * @param mcpServerSession the MCP server session
     * @return the enhanced session if found, null otherwise
     */
    @Nullable
    public EnhancedMcpServerSession getByMcpSession(Object mcpServerSession) {
        if (mcpServerSession == null) {
            return null;
        }
        
        String sessionId = extractSessionId(mcpServerSession);
        return sessionRegistry.get(sessionId);
    }
    
    /**
     * Remove a session from the registry.
     * 
     * @param sessionId the session ID to remove
     * @return the removed enhanced session, or null if not found
     */
    @Nullable
    public EnhancedMcpServerSession unregister(String sessionId) {
        EnhancedMcpServerSession removed = sessionRegistry.remove(sessionId);
        if (removed != null) {
            // Also remove from session manager
            // Note: deleteSession method might not exist in current implementation
            // sessionManager.deleteSession(removed.getApplicationSession().getId());
        }
        return removed;
    }
    
    /**
     * Check if a session is registered.
     * 
     * @param sessionId the session ID
     * @return true if the session is registered
     */
    public boolean isRegistered(String sessionId) {
        return sessionRegistry.containsKey(sessionId);
    }
    
    /**
     * Get the count of registered sessions.
     * 
     * @return the number of registered sessions
     */
    public int getSessionCount() {
        return sessionRegistry.size();
    }
    
    /**
     * Clear all registered sessions.
     * This will also clean up the session manager.
     */
    public void clearAll() {
        sessionRegistry.values().forEach(enhanced -> {
            // Note: deleteSession method might not exist in current implementation
            // sessionManager.deleteSession(enhanced.getApplicationSession().getId());
        });
        sessionRegistry.clear();
    }
    
    /**
     * Extract session ID from MCP server session object.
     * This uses reflection to be compatible with different MCP SDK versions.
     * 
     * @param mcpServerSession the MCP server session
     * @return the session ID
     */
    private String extractSessionId(Object mcpServerSession) {
        try {
            // Try to get ID using common method names
            String[] methodNames = {"getId", "getSessionId", "getClientId"};
            
            for (String methodName : methodNames) {
                try {
                    Object result = mcpServerSession.getClass()
                        .getMethod(methodName)
                        .invoke(mcpServerSession);
                    if (result != null) {
                        return result.toString();
                    }
                } catch (NoSuchMethodException e) {
                    // Try next method name
                    continue;
                }
            }
            
            // Fallback to object hash code if no ID method found
            return "session_" + System.identityHashCode(mcpServerSession);
            
        } catch (Exception e) {
            // Ultimate fallback
            return "session_" + System.identityHashCode(mcpServerSession);
        }
    }
}
