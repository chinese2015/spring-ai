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

import org.springframework.util.ClassUtils;

/**
 * Adaptive context holder that automatically selects the best
 * context propagation strategy based on the runtime environment.
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
public class AdaptiveContextHolder implements McpPlusContextHolder {
    
    private static final String CONTEXT_KEY = "MCP_PLUS_CONTEXT";
    
    private final ThreadLocal<McpPlusContext> threadLocalContext = new ThreadLocal<>();
    private final InheritableThreadLocal<McpPlusContext> inheritableContext = new InheritableThreadLocal<>();
    
    @Override
    public void setContext(McpPlusContext context) {
        EnvironmentDetector.Environment env = EnvironmentDetector.detectEnvironment();
        
        switch (env) {
            case SERVLET:
                threadLocalContext.set(context);
                setWebRequestAttribute(context);
                break;
            case REACTIVE:
                // For reactive environment, context should be set in reactive context
                // This method is mainly for compatibility
                threadLocalContext.set(context);
                setReactiveContext(context);
                break;
            case ASYNC:
                inheritableContext.set(context);
                break;
            default:
                threadLocalContext.set(context);
        }
    }
    
    @Override
    public McpPlusContext getContext() {
        EnvironmentDetector.Environment env = EnvironmentDetector.detectEnvironment();
        
        switch (env) {
            case SERVLET:
                return getServletContext();
            case REACTIVE:
                return getReactiveContext();
            case ASYNC:
                return getAsyncContext();
            default:
                return getDefaultContext();
        }
    }
    
    @Override
    public void clearContext() {
        threadLocalContext.remove();
        inheritableContext.remove();
        clearWebRequestAttribute();
        clearReactiveContext();
    }
    
    private McpPlusContext getServletContext() {
        McpPlusContext context = threadLocalContext.get();
        if (context == null) {
            // Try to extract from Spring Web RequestContext if available
            context = extractFromWebRequest();
        }
        return context != null ? context : McpPlusContext.empty();
    }
    
    private McpPlusContext getReactiveContext() {
        // In reactive environment, try to get from Reactor Context
        McpPlusContext context = getFromReactiveContext();
        if (context == null) {
            context = threadLocalContext.get();
        }
        return context != null ? context : McpPlusContext.empty();
    }
    
    private McpPlusContext getAsyncContext() {
        McpPlusContext context = inheritableContext.get();
        if (context == null) {
            context = threadLocalContext.get();
        }
        return context != null ? context : McpPlusContext.empty();
    }
    
    private McpPlusContext getDefaultContext() {
        McpPlusContext context = threadLocalContext.get();
        return context != null ? context : McpPlusContext.empty();
    }
    
    private McpPlusContext extractFromWebRequest() {
        try {
            // Try to extract from Spring Web RequestAttributes if available
            Class<?> requestContextHolderClass = ClassUtils.forName(
                "org.springframework.web.context.request.RequestContextHolder", null);
            Object requestAttributes = requestContextHolderClass.getMethod("getRequestAttributes").invoke(null);
            
            if (requestAttributes != null) {
                Object attribute = requestAttributes.getClass()
                    .getMethod("getAttribute", String.class, int.class)
                    .invoke(requestAttributes, CONTEXT_KEY, 0); // REQUEST_SCOPE
                
                if (attribute instanceof McpPlusContext) {
                    return (McpPlusContext) attribute;
                }
            }
        } catch (Exception e) {
            // Silently ignore - Spring Web might not be available
        }
        return null;
    }
    
    private void setWebRequestAttribute(McpPlusContext context) {
        try {
            Class<?> requestContextHolderClass = ClassUtils.forName(
                "org.springframework.web.context.request.RequestContextHolder", null);
            Object requestAttributes = requestContextHolderClass.getMethod("getRequestAttributes").invoke(null);
            
            if (requestAttributes != null) {
                requestAttributes.getClass()
                    .getMethod("setAttribute", String.class, Object.class, int.class)
                    .invoke(requestAttributes, CONTEXT_KEY, context, 0); // REQUEST_SCOPE
            }
        } catch (Exception e) {
            // Silently ignore - Spring Web might not be available
        }
    }
    
    private void clearWebRequestAttribute() {
        try {
            Class<?> requestContextHolderClass = ClassUtils.forName(
                "org.springframework.web.context.request.RequestContextHolder", null);
            Object requestAttributes = requestContextHolderClass.getMethod("getRequestAttributes").invoke(null);
            
            if (requestAttributes != null) {
                requestAttributes.getClass()
                    .getMethod("removeAttribute", String.class, int.class)
                    .invoke(requestAttributes, CONTEXT_KEY, 0); // REQUEST_SCOPE
            }
        } catch (Exception e) {
            // Silently ignore
        }
    }
    
    private McpPlusContext getFromReactiveContext() {
        try {
            // Try to get from Reactor context
            Class<?> contextClass = ClassUtils.forName("reactor.util.context.Context", null);
            // This is a simplified implementation - in real reactive environment,
            // the context would be passed through the reactive chain
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private void setReactiveContext(McpPlusContext context) {
        // In reactive environment, context is typically passed through the reactive chain
        // This is a placeholder for reactive context setting
    }
    
    private void clearReactiveContext() {
        // Clear reactive context if needed
    }
}

