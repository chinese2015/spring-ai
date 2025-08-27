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

import org.springframework.ai.mcp.plus.core.impl.DefaultMcpPlusContext;
import org.springframework.ai.mcp.plus.core.session.McpPlusSession;
import org.junit.jupiter.api.Test;
// import org.springframework.ai.chat.model.ToolContext; // Commented out for standalone demo

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for McpPlusContext implementations.
 * 
 * @author Spring AI MCP Plus Team
 */
class McpPlusContextTest {
    
    @Test
    void testEmptyContext() {
        McpPlusContext context = McpPlusContext.empty();
        
        assertNotNull(context);
        assertNull(context.getToolContext());
        assertNotNull(context.getSession());
        assertTrue(context.getSessionId().isPresent());
        assertNotNull(context.getRequestMetadata());
        assertTrue(context.getHeaders().isEmpty());
        assertTrue(context.getAuthToken().isEmpty());
        assertTrue(context.getClientId().isEmpty());
        assertTrue(context.getAttributes().isEmpty());
    }
    
    @Test
    void testContextFromToolContext() {
        Map<String, Object> toolContextMap = Map.of(
            "Authorization", "Bearer token123",
            "X-Client-ID", "client456",
            "customAttribute", "value"
        );
        ToolContext toolContext = new ToolContext(toolContextMap);
        
        McpPlusContext context = McpPlusContext.from(toolContext);
        
        assertNotNull(context);
        assertSame(toolContext, context.getToolContext());
        assertNotNull(context.getSession());
        
        // Should extract auth information
        Optional<String> authToken = context.getAuthToken();
        assertTrue(authToken.isPresent());
        assertTrue(authToken.get().contains("Bearer token123") || 
                  context.getAttribute("Authorization", String.class).isPresent());
        
        // Should have attributes from ToolContext
        assertTrue(context.getAttribute("customAttribute", String.class).isPresent());
        assertEquals("value", context.getAttribute("customAttribute", String.class).get());
    }
    
    @Test
    void testContextAttributes() {
        McpPlusContext context = McpPlusContext.empty();
        
        // Test setting and getting attributes
        context.setAttribute("key1", "value1");
        context.setAttribute("key2", 42);
        
        assertEquals("value1", context.getAttribute("key1", String.class).orElse(null));
        assertEquals(Integer.valueOf(42), context.getAttribute("key2", Integer.class).orElse(null));
        
        // Test wrong type
        assertFalse(context.getAttribute("key1", Integer.class).isPresent());
        
        // Test removal
        context.removeAttribute("key1");
        assertFalse(context.getAttribute("key1", String.class).isPresent());
        
        // Test null key
        context.setAttribute(null, "value");
        context.removeAttribute(null);
        assertFalse(context.getAttribute(null, String.class).isPresent());
    }
    
    @Test
    void testContextHeaders() {
        DefaultMcpPlusContext context = new DefaultMcpPlusContext();
        
        // Test setting headers
        context.setHeader("Content-Type", "application/json");
        context.setHeader("Authorization", "Bearer token");
        
        assertEquals("application/json", context.getHeader("Content-Type").orElse(null));
        assertEquals("Bearer token", context.getHeader("Authorization").orElse(null));
        
        // Test auth token extraction
        assertTrue(context.getAuthToken().isPresent());
        
        // Test header removal
        context.setHeader("Content-Type", null);
        assertFalse(context.getHeader("Content-Type").isPresent());
        
        // Test immutable map
        Map<String, String> headers = context.getHeaders();
        assertThrows(UnsupportedOperationException.class, () -> headers.put("test", "value"));
    }
    
    @Test
    void testSessionIntegration() {
        McpPlusContext context = McpPlusContext.empty();
        McpPlusSession session = context.getSession();
        
        assertNotNull(session);
        assertNotNull(session.getId());
        assertEquals(session.getId(), context.getSessionId().orElse(null));
        
        // Test session data access through context
        session.setData("testKey", "testValue");
        
        // Session should be the same instance
        assertSame(session, context.getSession());
    }
}
