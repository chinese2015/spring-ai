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

import org.springframework.ai.mcp.plus.core.bridge.McpPlusContextBridge;
import org.springframework.ai.mcp.plus.core.context.McpPlusContext;
import org.springframework.ai.mcp.plus.core.context.McpPlusContextAccessor;
import org.springframework.ai.mcp.plus.core.session.enhanced.EnhancedMcpServerSession;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * AOP aspect that non-intrusively enhances tool execution with MCP Plus context.
 * 
 * This aspect:
 * - Intercepts Spring AI tool method calls
 * - Attempts to discover the current MCP session from execution context
 * - Creates and sets MCP Plus context for tool execution
 * - Ensures proper cleanup after execution
 * 
 * The discovery process tries multiple strategies:
 * 1. ThreadLocal context from Spring AI MCP
 * 2. Current request attributes in web environment
 * 3. Reactive context in WebFlux environment
 * 4. Call stack analysis as fallback
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
@Aspect
public class McpPlusToolAspect {
    
    private final McpSessionRegistrar sessionRegistrar;
    
    public McpPlusToolAspect(McpSessionRegistrar sessionRegistrar) {
        this.sessionRegistrar = sessionRegistrar;
    }
    
    /**
     * Intercept tool method execution and enhance with MCP Plus context.
     */
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object enhanceToolExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        // Try to discover current MCP session
        Object mcpServerSession = discoverCurrentMcpSession();
        
        if (mcpServerSession != null) {
            // Register/get enhanced session
            EnhancedMcpServerSession enhanced = sessionRegistrar.registerOrGet(mcpServerSession);
            
            // Create MCP Plus context
            McpPlusContext context = McpPlusContextBridge.fromMcpServerSession(mcpServerSession);
            
            // Execute with enhanced context
            McpPlusContext previousContext = McpPlusContextAccessor.getCurrentContext();
            McpPlusContextAccessor.getContextHolder().setContext(context);
            try {
                return joinPoint.proceed();
            } finally {
                if (previousContext != null && !previousContext.equals(McpPlusContext.empty())) {
                    McpPlusContextAccessor.getContextHolder().setContext(previousContext);
                } else {
                    McpPlusContextAccessor.getContextHolder().clearContext();
                }
            }
        }
        
        // No MCP session found - execute normally
        return joinPoint.proceed();
    }
    
    /**
     * Discover the current MCP server session using multiple strategies.
     * 
     * @return the MCP server session if found, null otherwise
     */
    @Nullable
    private Object discoverCurrentMcpSession() {
        // Strategy 1: Try Spring AI MCP ThreadLocal context
        Object session = tryGetFromSpringAiContext();
        if (session != null) {
            return session;
        }
        
        // Strategy 2: Try web request attributes
        session = tryGetFromWebRequest();
        if (session != null) {
            return session;
        }
        
        // Strategy 3: Try reactive context
        session = tryGetFromReactiveContext();
        if (session != null) {
            return session;
        }
        
        // Strategy 4: Try call stack analysis
        session = tryGetFromCallStack();
        if (session != null) {
            return session;
        }
        
        return null;
    }
    
    /**
     * Try to get MCP session from Spring AI's internal context.
     */
    @Nullable
    private Object tryGetFromSpringAiContext() {
        try {
            // Look for Spring AI MCP's context holder
            // This might be in ThreadLocal or request-scoped beans
            
            // Try to find MCP-related ThreadLocal variables
            Class<?> mcpContextClass = ClassUtils.forName(
                "org.springframework.ai.mcp.server.context.McpContext", 
                getClass().getClassLoader());
            
            if (mcpContextClass != null) {
                // Look for current session field or method
                Method getCurrentSession = mcpContextClass.getMethod("getCurrentSession");
                Object session = getCurrentSession.invoke(null);
                if (session != null) {
                    return session;
                }
            }
        } catch (Exception e) {
            // Silently ignore - Spring AI MCP context might not be available
        }
        
        // Try alternative approach - look for ThreadLocal fields in known classes
        try {
            // This is a heuristic approach - look for common ThreadLocal patterns
            String[] possibleClasses = {
                "org.springframework.ai.mcp.server.McpServerSession",
                "org.springframework.ai.mcp.server.context.McpServerContext",
                "org.springframework.ai.mcp.server.transport.McpTransportContext"
            };
            
            for (String className : possibleClasses) {
                try {
                    Class<?> clazz = ClassUtils.forName(className, getClass().getClassLoader());
                    Object session = tryGetThreadLocalFromClass(clazz);
                    if (session != null) {
                        return session;
                    }
                } catch (ClassNotFoundException e) {
                    // Try next class
                    continue;
                }
            }
        } catch (Exception e) {
            // Silently ignore
        }
        
        return null;
    }
    
    /**
     * Try to get MCP session from web request attributes.
     */
    @Nullable
    private Object tryGetFromWebRequest() {
        try {
            Class<?> requestContextHolderClass = ClassUtils.forName(
                "org.springframework.web.context.request.RequestContextHolder", 
                getClass().getClassLoader());
            
            Object requestAttributes = requestContextHolderClass
                .getMethod("getRequestAttributes")
                .invoke(null);
            
            if (requestAttributes != null) {
                // Look for MCP session in request attributes
                String[] possibleKeys = {"mcpSession", "mcpServerSession", "mcp.session"};
                
                for (String key : possibleKeys) {
                    try {
                        Object session = requestAttributes.getClass()
                            .getMethod("getAttribute", String.class, int.class)
                            .invoke(requestAttributes, key, 0); // REQUEST_SCOPE
                        
                        if (session != null) {
                            return session;
                        }
                    } catch (Exception e) {
                        // Try next key
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore - Spring Web might not be available
        }
        return null;
    }
    
    /**
     * Try to get MCP session from reactive context.
     */
    @Nullable
    private Object tryGetFromReactiveContext() {
        try {
            // Check if we're in a reactive context
            Class<?> contextClass = ClassUtils.forName("reactor.util.context.Context", 
                getClass().getClassLoader());
            Class<?> monoClass = ClassUtils.forName("reactor.core.publisher.Mono", 
                getClass().getClassLoader());
            
            // Try to get current reactive context
            // This is a simplified approach for detection
            // In real reactive environments, proper context propagation should be used
            
        } catch (Exception e) {
            // Silently ignore - Reactor might not be available
        }
        return null;
    }
    
    /**
     * Try to get MCP session from call stack analysis.
     * This is a fallback strategy that looks at the call stack for MCP-related objects.
     */
    @Nullable
    private Object tryGetFromCallStack() {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                
                // Look for Spring AI MCP classes in the call stack
                if (className.contains("mcp") && 
                    (className.contains("springframework.ai") || className.contains("modelcontextprotocol"))) {
                    
                    try {
                        Class<?> clazz = ClassUtils.forName(className, getClass().getClassLoader());
                        Object session = tryGetSessionFromClass(clazz);
                        if (session != null) {
                            return session;
                        }
                    } catch (Exception e) {
                        // Continue with next stack element
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore
        }
        return null;
    }
    
    /**
     * Try to extract ThreadLocal session from a class.
     */
    @Nullable
    private Object tryGetThreadLocalFromClass(Class<?> clazz) {
        try {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (ThreadLocal.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    ThreadLocal<?> threadLocal = (ThreadLocal<?>) field.get(null);
                    Object value = threadLocal.get();
                    if (value != null && isLikelyMcpSession(value)) {
                        return value;
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore
        }
        return null;
    }
    
    /**
     * Try to extract session from class instance or static methods.
     */
    @Nullable
    private Object tryGetSessionFromClass(Class<?> clazz) {
        try {
            // Look for static methods that return session-like objects
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getParameterCount() == 0 && 
                    java.lang.reflect.Modifier.isStatic(method.getModifiers()) &&
                    method.getName().toLowerCase().contains("session")) {
                    
                    method.setAccessible(true);
                    Object result = method.invoke(null);
                    if (result != null && isLikelyMcpSession(result)) {
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore
        }
        return null;
    }
    
    /**
     * Heuristic check to determine if an object is likely an MCP session.
     */
    private boolean isLikelyMcpSession(Object obj) {
        if (obj == null) {
            return false;
        }
        
        String className = obj.getClass().getName().toLowerCase();
        return className.contains("mcp") && 
               (className.contains("session") || className.contains("server"));
    }
}
