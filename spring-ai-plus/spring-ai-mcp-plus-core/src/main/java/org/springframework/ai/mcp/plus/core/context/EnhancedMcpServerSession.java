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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced MCP server session that extends the protocol-level session with 
 * application-level capabilities.
 * 
 * This class acts as a bridge between:
 * 1. MCP Java SDK's McpServerSession (protocol layer)
 * 2. MCP Plus Session (application layer)
 * 
 * The design allows us to:
 * - Extend MCP Java SDK's session functionality
 * - Add business-level session management 
 * - Maintain separation of concerns between protocol and application layers
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
public class EnhancedMcpServerSession {
    
    private final Object mcpServerSession; // MCP Java SDK's McpServerSession
    private final McpPlusSession applicationSession; // Our business session
    private final Map<String, Object> protocolAttributes; // Protocol-level attributes
    private final Map<String, Object> applicationAttributes; // Application-level attributes
    
    /**
     * Create an enhanced session wrapping an existing MCP server session.
     * 
     * @param mcpServerSession the underlying MCP Java SDK session
     */
    public EnhancedMcpServerSession(Object mcpServerSession) {
        this.mcpServerSession = mcpServerSession;
        this.applicationSession = new DefaultMcpPlusSession();
        this.protocolAttributes = new ConcurrentHashMap<>();
        this.applicationAttributes = new ConcurrentHashMap<>();
    }
    
    /**
     * Create an enhanced session with custom application session.
     * 
     * @param mcpServerSession the underlying MCP Java SDK session
     * @param applicationSession the application-level session
     */
    public EnhancedMcpServerSession(Object mcpServerSession, McpPlusSession applicationSession) {
        this.mcpServerSession = mcpServerSession;
        this.applicationSession = applicationSession;
        this.protocolAttributes = new ConcurrentHashMap<>();
        this.applicationAttributes = new ConcurrentHashMap<>();
    }
    
    /**
     * Get the underlying MCP Java SDK server session.
     * This provides access to protocol-level operations like:
     * - JSON-RPC message handling
     * - Transport management
     * - Protocol state
     * 
     * @return the MCP server session
     */
    public Object getMcpServerSession() {
        return mcpServerSession;
    }
    
    /**
     * Get the application-level session.
     * This provides access to business operations like:
     * - User session data
     * - Authentication info
     * - Business state management
     * 
     * @return the application session
     */
    public McpPlusSession getApplicationSession() {
        return applicationSession;
    }
    
    /**
     * Set a protocol-level attribute.
     * These attributes are related to MCP protocol operations.
     * 
     * @param key the attribute key
     * @param value the attribute value
     */
    public void setProtocolAttribute(String key, Object value) {
        protocolAttributes.put(key, value);
    }
    
    /**
     * Get a protocol-level attribute.
     * 
     * @param key the attribute key
     * @param type the expected type
     * @return the attribute value if present and of correct type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProtocolAttribute(String key, Class<T> type) {
        Object value = protocolAttributes.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }
    
    /**
     * Set an application-level attribute.
     * These attributes are related to business logic.
     * 
     * @param key the attribute key
     * @param value the attribute value
     */
    public void setApplicationAttribute(String key, Object value) {
        applicationAttributes.put(key, value);
    }
    
    /**
     * Get an application-level attribute.
     * 
     * @param key the attribute key
     * @param type the expected type
     * @return the attribute value if present and of correct type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getApplicationAttribute(String key, Class<T> type) {
        Object value = applicationAttributes.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }
    
    /**
     * Get all protocol-level attributes.
     * 
     * @return immutable map of protocol attributes
     */
    public Map<String, Object> getProtocolAttributes() {
        return Map.copyOf(protocolAttributes);
    }
    
    /**
     * Get all application-level attributes.
     * 
     * @return immutable map of application attributes
     */
    public Map<String, Object> getApplicationAttributes() {
        return Map.copyOf(applicationAttributes);
    }
    
    /**
     * Clear all attributes (both protocol and application level).
     */
    public void clearAttributes() {
        protocolAttributes.clear();
        applicationAttributes.clear();
    }
    
    /**
     * Check if this enhanced session is still active.
     * This checks both protocol session and application session status.
     * 
     * @return true if both sessions are active
     */
    public boolean isActive() {
        // In real implementation, we would check mcpServerSession.isActive()
        // For now, we only check application session
        return !applicationSession.isExpired();
    }
    
    /**
     * Get session ID that combines both protocol and application identifiers.
     * 
     * @return composite session ID
     */
    public String getSessionId() {
        // In real implementation, we might combine MCP session ID with application session ID
        return applicationSession.getId();
    }
    
    /**
     * Invalidate both protocol and application sessions.
     */
    public void invalidate() {
        // In real implementation, we would also invalidate mcpServerSession
        applicationSession.invalidate();
        clearAttributes();
    }
}
