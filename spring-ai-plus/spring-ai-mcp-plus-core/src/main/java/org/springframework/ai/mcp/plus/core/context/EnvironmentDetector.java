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
 * Detects the current runtime environment to determine the appropriate
 * context propagation strategy.
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
public class EnvironmentDetector {
    
    private static final String REACTOR_CONTEXT_CLASS = "reactor.util.context.Context";
    private static final String SERVLET_REQUEST_CLASS = "jakarta.servlet.http.HttpServletRequest";
    private static final String LEGACY_SERVLET_REQUEST_CLASS = "javax.servlet.http.HttpServletRequest";
    private static final String SPRING_WEBFLUX_CLASS = "org.springframework.web.reactive.function.server.ServerRequest";
    private static final String SPRING_WEB_REQUEST_CLASS = "org.springframework.web.context.request.RequestContextHolder";
    
    /**
     * Represents the detected runtime environment.
     */
    public enum Environment {
        /**
         * Traditional servlet-based environment (Spring MVC)
         */
        SERVLET,
        
        /**
         * Reactive environment (Spring WebFlux)
         */
        REACTIVE,
        
        /**
         * Asynchronous environment with thread inheritance needs
         */
        ASYNC,
        
        /**
         * Unknown or default environment
         */
        DEFAULT
    }
    
    /**
     * Detect the current runtime environment.
     * 
     * @return the detected environment
     */
    public static Environment detectEnvironment() {
        // Check for reactive environment
        if (isReactiveEnvironment()) {
            return Environment.REACTIVE;
        }
        
        // Check for servlet environment
        if (isServletEnvironment()) {
            return Environment.SERVLET;
        }
        
        // Check for async environment
        if (isAsyncEnvironment()) {
            return Environment.ASYNC;
        }
        
        return Environment.DEFAULT;
    }
    
    private static boolean isReactiveEnvironment() {
        try {
            // Check if Reactor is on classpath
            ClassUtils.forName(REACTOR_CONTEXT_CLASS, null);
            
            // Check if WebFlux is on classpath
            ClassUtils.forName(SPRING_WEBFLUX_CLASS, null);
            
            // Additional checks could be added here to determine if we're
            // actually in a reactive call stack
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private static boolean isServletEnvironment() {
        try {
            // Check if Servlet API is on classpath (Jakarta or legacy)
            boolean servletAvailable = false;
            try {
                ClassUtils.forName(SERVLET_REQUEST_CLASS, null);
                servletAvailable = true;
            } catch (ClassNotFoundException e) {
                try {
                    ClassUtils.forName(LEGACY_SERVLET_REQUEST_CLASS, null);
                    servletAvailable = true;
                } catch (ClassNotFoundException ex) {
                    // Neither available
                }
            }
            
            if (!servletAvailable) {
                return false;
            }
            
            // Check if Spring Web is available
            ClassUtils.forName(SPRING_WEB_REQUEST_CLASS, null);
            
            // Check if we have a current request context
            try {
                Class<?> requestContextHolderClass = ClassUtils.forName(SPRING_WEB_REQUEST_CLASS, null);
                Object requestAttributes = requestContextHolderClass.getMethod("getRequestAttributes").invoke(null);
                return requestAttributes != null;
            } catch (Exception e) {
                // If we can't check, assume servlet environment if classes are available
                return true;
            }
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private static boolean isAsyncEnvironment() {
        // Check if we're in a thread pool or async context
        Thread currentThread = Thread.currentThread();
        String threadName = currentThread.getName();
        
        // Common async thread pool patterns
        return threadName.contains("pool") ||
               threadName.contains("async") ||
               threadName.contains("executor") ||
               threadName.startsWith("ForkJoinPool") ||
               threadName.contains("TaskExecutor");
    }
    
    /**
     * Check if the current environment supports context inheritance.
     * 
     * @return true if context inheritance is supported
     */
    public static boolean supportsContextInheritance() {
        Environment env = detectEnvironment();
        return env == Environment.ASYNC || env == Environment.REACTIVE;
    }
    
    /**
     * Check if the current environment is web-based.
     * 
     * @return true if web-based environment
     */
    public static boolean isWebEnvironment() {
        Environment env = detectEnvironment();
        return env == Environment.SERVLET || env == Environment.REACTIVE;
    }
}

