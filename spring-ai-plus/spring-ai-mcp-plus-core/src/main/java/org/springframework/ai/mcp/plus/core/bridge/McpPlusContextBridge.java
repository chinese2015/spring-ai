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

package org.springframework.ai.mcp.plus.core.bridge;

import org.springframework.ai.mcp.plus.core.context.McpPlusContext;
import org.springframework.ai.mcp.plus.core.context.ToolContext;
import org.springframework.ai.mcp.plus.core.impl.DefaultMcpPlusContext;
import org.springframework.ai.mcp.plus.core.metadata.impl.DefaultRequestMetadata;
import org.springframework.ai.mcp.plus.core.session.enhanced.EnhancedMcpServerSession;
import org.springframework.ai.mcp.plus.core.metadata.RequestMetadata;
import org.springframework.ai.mcp.plus.core.session.McpPlusSession;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Bridge between different context layers in the MCP Plus ecosystem.
 * 
 * This class provides integration between:
 * 1. MCP Java SDK context (protocol layer)
 * 2. Spring AI ToolContext (tool execution layer)  
 * 3. MCP Plus Context (enhanced application layer)
 * 
 * The bridge ensures that context information flows properly between
 * these different layers while maintaining clear separation of concerns.
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
public class McpPlusContextBridge {
    
    /**
     * Create an MCP Plus context from Spring AI ToolContext.
     * This enhances the basic ToolContext with additional capabilities.
     * 
     * @param toolContext the Spring AI ToolContext
     * @return enhanced MCP Plus context
     */
    public static McpPlusContext fromToolContext(@Nullable ToolContext toolContext) {
        return new DefaultMcpPlusContext(toolContext);
    }
    
    /**
     * Create an MCP Plus context from MCP server session.
     * This bridges protocol-level session with application-level context.
     * 
     * @param mcpServerSession the MCP Java SDK server session
     * @return enhanced MCP Plus context
     */
    public static McpPlusContext fromMcpServerSession(@Nullable Object mcpServerSession) {
        if (mcpServerSession == null) {
            return McpPlusContext.empty();
        }
        
        // Create enhanced session that wraps the MCP server session
        EnhancedMcpServerSession enhancedSession = new EnhancedMcpServerSession(mcpServerSession);
        
        // Create context with the enhanced session
        return new DefaultMcpPlusContext(null, enhancedSession.getApplicationSession(), 
                                       new DefaultRequestMetadata("MCP"), Map.of(), Map.of());
    }
    
    /**
     * Create an MCP Plus context that bridges both Spring AI and MCP SDK contexts.
     * This is the most comprehensive context that has access to all layers.
     * 
     * @param toolContext the Spring AI ToolContext
     * @param mcpServerSession the MCP Java SDK server session
     * @return comprehensive MCP Plus context
     */
    public static McpPlusContext fromBothContexts(@Nullable ToolContext toolContext, 
                                                 @Nullable Object mcpServerSession) {
        if (mcpServerSession == null) {
            return fromToolContext(toolContext);
        }
        
        // Create enhanced session
        EnhancedMcpServerSession enhancedSession = new EnhancedMcpServerSession(mcpServerSession);
        
        // Extract any existing data from ToolContext
        Map<String, Object> attributes = Map.of();
        if (toolContext != null) {
            attributes = toolContext.getContext();
        }
        
        // Create comprehensive context
        return new DefaultMcpPlusContext(toolContext, enhancedSession.getApplicationSession(),
                                       new DefaultRequestMetadata("MCP"), Map.of(), attributes);
    }
    
    /**
     * Extract Spring AI ToolContext from MCP Plus context.
     * This allows backward compatibility with existing Spring AI tools.
     * 
     * @param mcpPlusContext the MCP Plus context
     * @return Spring AI ToolContext if available
     */
    public static Optional<ToolContext> toToolContext(McpPlusContext mcpPlusContext) {
        return Optional.ofNullable(mcpPlusContext.getToolContext());
    }
    
    /**
     * Extract MCP server session from MCP Plus context.
     * This provides access to protocol-level operations.
     * 
     * @param mcpPlusContext the MCP Plus context
     * @return MCP server session if available
     */
    public static Optional<Object> toMcpServerSession(McpPlusContext mcpPlusContext) {
        return mcpPlusContext.getSession().getUnderlyingMcpServerSession();
    }
    
    /**
     * Check if the context has MCP server session integration.
     * 
     * @param mcpPlusContext the context to check
     * @return true if MCP server session is available
     */
    public static boolean hasMcpServerSession(McpPlusContext mcpPlusContext) {
        return toMcpServerSession(mcpPlusContext).isPresent();
    }
    
    /**
     * Check if the context has Spring AI ToolContext integration.
     * 
     * @param mcpPlusContext the context to check
     * @return true if ToolContext is available
     */
    public static boolean hasToolContext(McpPlusContext mcpPlusContext) {
        return toToolContext(mcpPlusContext).isPresent();
    }
    
    /**
     * Merge context information from multiple sources into a single MCP Plus context.
     * This is useful when integrating contexts from different parts of the application.
     * 
     * @param primary the primary context (takes precedence)
     * @param secondary the secondary context (provides fallback values)
     * @return merged context
     */
    public static McpPlusContext merge(McpPlusContext primary, McpPlusContext secondary) {
        // Start with primary context attributes
        Map<String, Object> mergedAttributes = primary.getAttributes();
        
        // Add secondary attributes that don't exist in primary
        secondary.getAttributes().forEach((key, value) -> {
            mergedAttributes.putIfAbsent(key, value);
        });
        
        // Use primary session and metadata, with secondary as fallback
        McpPlusSession session = primary.getSession() != null ? primary.getSession() : secondary.getSession();
        RequestMetadata metadata = primary.getRequestMetadata() != null ? 
            primary.getRequestMetadata() : secondary.getRequestMetadata();
        
        // Merge headers
        Map<String, String> mergedHeaders = primary.getHeaders();
        secondary.getHeaders().forEach((key, value) -> {
            mergedHeaders.putIfAbsent(key, value);
        });
        
        return new DefaultMcpPlusContext(primary.getToolContext(), session, metadata, mergedHeaders, mergedAttributes);
    }
}
