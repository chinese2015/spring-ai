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

package org.springframework.ai.mcp.plus.core.impl;

// import org.springframework.ai.chat.model.ToolContext; // Commented out for standalone demo
import org.springframework.ai.mcp.plus.core.context.McpPlusContext;
import org.springframework.ai.mcp.plus.core.context.ToolContext;
import org.springframework.ai.mcp.plus.core.metadata.RequestMetadata;
import org.springframework.ai.mcp.plus.core.metadata.impl.DefaultRequestMetadata;
import org.springframework.ai.mcp.plus.core.session.McpPlusSession;
import org.springframework.ai.mcp.plus.core.session.impl.DefaultMcpPlusSession;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of McpPlusContext.
 * Thread-safe implementation that wraps Spring AI ToolContext and provides additional functionality.
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
public class DefaultMcpPlusContext implements McpPlusContext {
    
    private static final String AUTH_TOKEN_HEADER = "Authorization";
    private static final String CLIENT_ID_HEADER = "X-Client-ID";
    
    private final ToolContext toolContext;
    private final McpPlusSession session;
    private final RequestMetadata requestMetadata;
    private final Map<String, String> headers;
    private final Map<String, Object> attributes;
    
    /**
     * Create an empty context.
     */
    public DefaultMcpPlusContext() {
        this(null);
    }
    
    /**
     * Create a context wrapping the given ToolContext.
     * 
     * @param toolContext the Spring AI ToolContext to wrap
     */
    public DefaultMcpPlusContext(@Nullable ToolContext toolContext) {
        this.toolContext = toolContext;
        this.session = new DefaultMcpPlusSession();
        this.requestMetadata = new DefaultRequestMetadata("UNKNOWN");
        this.headers = new ConcurrentHashMap<>();
        this.attributes = new ConcurrentHashMap<>();
        
        // Extract information from ToolContext if available
        if (toolContext != null) {
            extractFromToolContext(toolContext);
        }
    }
    
    /**
     * Create a context with all components specified.
     * 
     * @param toolContext the Spring AI ToolContext
     * @param session the session
     * @param requestMetadata the request metadata
     * @param headers the HTTP headers
     * @param attributes the context attributes
     */
    public DefaultMcpPlusContext(@Nullable ToolContext toolContext, 
                                McpPlusSession session,
                                RequestMetadata requestMetadata,
                                Map<String, String> headers,
                                Map<String, Object> attributes) {
        this.toolContext = toolContext;
        this.session = session != null ? session : new DefaultMcpPlusSession();
        this.requestMetadata = requestMetadata != null ? requestMetadata : new DefaultRequestMetadata("UNKNOWN");
        this.headers = new ConcurrentHashMap<>(headers != null ? headers : Collections.emptyMap());
        this.attributes = new ConcurrentHashMap<>(attributes != null ? attributes : Collections.emptyMap());
    }
    
    @Override
    @Nullable
    public ToolContext getToolContext() {
        return toolContext;
    }
    
    @Override
    public McpPlusSession getSession() {
        return session;
    }
    
    @Override
    public Optional<String> getSessionId() {
        return Optional.of(session.getId());
    }
    
    @Override
    public RequestMetadata getRequestMetadata() {
        return requestMetadata;
    }
    
    @Override
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }
    
    @Override
    public Optional<String> getHeader(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(headers.get(name));
    }
    
    @Override
    public Optional<String> getAuthToken() {
        return getHeader(AUTH_TOKEN_HEADER)
                .or(() -> getAttribute("authToken", String.class));
    }
    
    @Override
    public Optional<String> getClientId() {
        return getHeader(CLIENT_ID_HEADER)
                .or(() -> getAttribute("clientId", String.class));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAttribute(String name, Class<T> type) {
        if (name == null || type == null) {
            return Optional.empty();
        }
        
        Object value = attributes.get(name);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        
        return Optional.empty();
    }
    
    @Override
    public void setAttribute(String name, Object value) {
        if (name == null) {
            return;
        }
        
        if (value == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, value);
        }
    }
    
    @Override
    public void removeAttribute(String name) {
        if (name != null) {
            attributes.remove(name);
        }
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
    
    /**
     * Set HTTP headers.
     * 
     * @param headers the headers to set
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers.clear();
        if (headers != null) {
            this.headers.putAll(headers);
        }
    }
    
    /**
     * Set a specific HTTP header.
     * 
     * @param name the header name
     * @param value the header value
     */
    public void setHeader(String name, String value) {
        if (name == null) {
            return;
        }
        
        if (value == null) {
            headers.remove(name);
        } else {
            headers.put(name, value);
        }
    }
    
    private void extractFromToolContext(ToolContext toolContext) {
        Map<String, Object> context = toolContext.getContext();
        if (context != null) {
            // Extract common attributes that might be in ToolContext
            context.forEach((key, value) -> {
                if (value instanceof String) {
                    // Check if it looks like a header
                    if (key.toLowerCase().contains("header") || 
                        key.equalsIgnoreCase(AUTH_TOKEN_HEADER) ||
                        key.equalsIgnoreCase(CLIENT_ID_HEADER)) {
                        headers.put(key, (String) value);
                    }
                }
                // Store everything as attributes
                attributes.put(key, value);
            });
        }
    }
    
    @Override
    public String toString() {
        return "DefaultMcpPlusContext{" +
               "sessionId=" + getSessionId().orElse("unknown") +
               ", transportType=" + requestMetadata.getTransportType() +
               ", headersCount=" + headers.size() +
               ", attributesCount=" + attributes.size() +
               ", hasToolContext=" + (toolContext != null) +
               '}';
    }
}
