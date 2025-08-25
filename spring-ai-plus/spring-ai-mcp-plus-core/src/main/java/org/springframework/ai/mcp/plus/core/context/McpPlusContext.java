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

// import org.springframework.ai.chat.model.ToolContext; // Commented out for standalone demo
import org.springframework.lang.Nullable;
import java.util.Map;
import java.util.Optional;

/**
 * Enhanced context for MCP Plus tools execution.
 * Extends Spring AI ToolContext with additional capabilities for session management,
 * request metadata access, and authentication information.
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
public interface McpPlusContext {
    
    /**
     * Get the underlying Spring AI ToolContext.
     * This provides compatibility with existing Spring AI tool infrastructure.
     * 
     * @return the Spring AI ToolContext, may be null if not available
     */
    @Nullable
    ToolContext getToolContext();
    
    /**
     * Get the current session associated with this context.
     * 
     * @return the current session
     */
    McpPlusSession getSession();
    
    /**
     * Get the session ID.
     * 
     * @return the session ID if available
     */
    Optional<String> getSessionId();
    
    /**
     * Get request metadata containing information about the current request.
     * 
     * @return the request metadata
     */
    RequestMetadata getRequestMetadata();
    
    /**
     * Get HTTP headers if this context is associated with an HTTP request.
     * 
     * @return map of HTTP headers, empty if not available
     */
    Map<String, String> getHeaders();
    
    /**
     * Get a specific HTTP header value.
     * 
     * @param name the header name
     * @return the header value if present
     */
    Optional<String> getHeader(String name);
    
    /**
     * Get the authentication token associated with this request.
     * 
     * @return the authentication token if present
     */
    Optional<String> getAuthToken();
    
    /**
     * Get the client ID that made this request.
     * 
     * @return the client ID if present
     */
    Optional<String> getClientId();
    
    /**
     * Get a typed attribute from the context.
     * 
     * @param <T> the expected type of the attribute
     * @param name the attribute name
     * @param type the expected class of the attribute
     * @return the attribute value if present and of the correct type
     */
    <T> Optional<T> getAttribute(String name, Class<T> type);
    
    /**
     * Set an attribute in the context.
     * 
     * @param name the attribute name
     * @param value the attribute value
     */
    void setAttribute(String name, Object value);
    
    /**
     * Remove an attribute from the context.
     * 
     * @param name the attribute name
     */
    void removeAttribute(String name);
    
    /**
     * Get all attributes as an immutable map.
     * 
     * @return all attributes
     */
    Map<String, Object> getAttributes();
    
    /**
     * Create a McpPlusContext from an existing Spring AI ToolContext.
     * 
     * @param toolContext the Spring AI ToolContext
     * @return a new McpPlusContext wrapping the ToolContext
     */
    static McpPlusContext from(@Nullable ToolContext toolContext) {
        return new DefaultMcpPlusContext(toolContext);
    }
    
    /**
     * Create an empty McpPlusContext.
     * 
     * @return a new empty McpPlusContext
     */
    static McpPlusContext empty() {
        return new DefaultMcpPlusContext();
    }
}
