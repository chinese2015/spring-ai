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

/**
 * Strategy interface for holding and managing McpPlusContext instances.
 * Different implementations can provide different context propagation strategies.
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
public interface McpPlusContextHolder {
    
    /**
     * Set the current context.
     * 
     * @param context the context to set
     */
    void setContext(McpPlusContext context);
    
    /**
     * Get the current context.
     * 
     * @return the current context, or an empty context if none is set
     */
    McpPlusContext getContext();
    
    /**
     * Clear the current context.
     */
    void clearContext();
    
    /**
     * Check if a context is currently set.
     * 
     * @return true if a context is set
     */
    default boolean hasContext() {
        McpPlusContext context = getContext();
        return context != null && context != McpPlusContext.empty();
    }
}

