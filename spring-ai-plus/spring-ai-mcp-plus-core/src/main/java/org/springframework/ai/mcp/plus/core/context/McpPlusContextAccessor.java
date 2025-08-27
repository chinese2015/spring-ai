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
import org.springframework.ai.mcp.plus.core.holder.McpPlusContextHolder;
import org.springframework.ai.mcp.plus.core.holder.impl.AdaptiveContextHolder;
import org.springframework.ai.mcp.plus.core.session.McpPlusSession;
import org.springframework.lang.Nullable;
import java.util.Optional;

/**
 * Convenient static accessor for McpPlusContext functionality.
 * Provides easy access to context, session, and common operations.
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
public final class McpPlusContextAccessor {
    
    private static volatile McpPlusContextHolder contextHolder = new AdaptiveContextHolder();
    
    private McpPlusContextAccessor() {
        // Utility class
    }
    
    /**
     * Set the context holder strategy.
     * 
     * @param contextHolder the context holder to use
     */
    public static void setContextHolder(McpPlusContextHolder contextHolder) {
        McpPlusContextAccessor.contextHolder = contextHolder;
    }
    
    /**
     * Get the current context holder.
     * 
     * @return the current context holder
     */
    public static McpPlusContextHolder getContextHolder() {
        return contextHolder;
    }
    
    /**
     * Get the current context.
     * 
     * @return the current context
     */
    public static McpPlusContext getCurrentContext() {
        return contextHolder.getContext();
    }
    
    /**
     * Get the current session.
     * 
     * @return the current session
     */
    public static McpPlusSession getCurrentSession() {
        return getCurrentContext().getSession();
    }
    
    /**
     * Get the authentication token from the current context.
     * 
     * @return the authentication token if available
     */
    public static Optional<String> getAuthToken() {
        return getCurrentContext().getAuthToken();
    }
    
    /**
     * Get session data of a specific type.
     * 
     * @param <T> the expected type
     * @param key the data key
     * @param type the expected class
     * @return the session data if available
     */
    public static <T> Optional<T> getSessionData(String key, Class<T> type) {
        return getCurrentSession().getData(key, type);
    }
    
    /**
     * Set session data.
     * 
     * @param key the data key
     * @param value the data value
     */
    public static void setSessionData(String key, Object value) {
        getCurrentSession().setData(key, value);
    }
    
    /**
     * Get the Spring AI ToolContext from the current context.
     * 
     * @return the Spring AI ToolContext if available
     */
    @Nullable
    public static ToolContext getSpringAiToolContext() {
        return getCurrentContext().getToolContext();
    }
    
    /**
     * Get a context attribute.
     * 
     * @param <T> the expected type
     * @param name the attribute name
     * @param type the expected class
     * @return the attribute value if available
     */
    public static <T> Optional<T> getContextAttribute(String name, Class<T> type) {
        return getCurrentContext().getAttribute(name, type);
    }
    
    /**
     * Set a context attribute.
     * 
     * @param name the attribute name
     * @param value the attribute value
     */
    public static void setContextAttribute(String name, Object value) {
        getCurrentContext().setAttribute(name, value);
    }
    
    /**
     * Clear the current context.
     */
    public static void clearContext() {
        contextHolder.clearContext();
    }
    
    /**
     * Check if there is a current context set.
     * 
     * @return true if a context is set
     */
    public static boolean hasContext() {
        return contextHolder.hasContext();
    }
    
    /**
     * Execute a runnable with a specific context.
     * 
     * @param context the context to use
     * @param runnable the code to execute
     */
    public static void runWithContext(McpPlusContext context, Runnable runnable) {
        McpPlusContext previousContext = contextHolder.getContext();
        try {
            contextHolder.setContext(context);
            runnable.run();
        } finally {
            if (previousContext != null && previousContext != McpPlusContext.empty()) {
                contextHolder.setContext(previousContext);
            } else {
                contextHolder.clearContext();
            }
        }
    }
    
    /**
     * Execute a supplier with a specific context and return the result.
     * 
     * @param <T> the return type
     * @param context the context to use
     * @param supplier the code to execute
     * @return the result of the supplier
     */
    public static <T> T callWithContext(McpPlusContext context, java.util.function.Supplier<T> supplier) {
        McpPlusContext previousContext = contextHolder.getContext();
        try {
            contextHolder.setContext(context);
            return supplier.get();
        } finally {
            if (previousContext != null && previousContext != McpPlusContext.empty()) {
                contextHolder.setContext(previousContext);
            } else {
                contextHolder.clearContext();
            }
        }
    }
}
