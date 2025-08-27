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

package org.springframework.ai.mcp.plus.autoconfigure;

import org.springframework.ai.mcp.plus.core.context.McpPlusContext;
import org.springframework.ai.mcp.plus.core.environment.EnvironmentDetector;
import org.springframework.ai.mcp.plus.core.holder.McpPlusContextHolder;
import org.springframework.ai.mcp.plus.core.holder.impl.AdaptiveContextHolder;
import org.springframework.ai.mcp.plus.core.session.McpPlusSessionManager;
import org.springframework.ai.mcp.plus.core.session.impl.DefaultSessionManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for Spring AI MCP Plus features.
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(McpPlusContext.class)
@EnableConfigurationProperties(McpPlusProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.mcp.plus", name = "enabled", havingValue = "true", matchIfMissing = true)
public class McpPlusAutoConfiguration {
    
    /**
     * Context-related configuration.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "spring.ai.mcp.plus.context", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ContextConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public McpPlusContextHolder mcpPlusContextHolder(McpPlusProperties properties) {
            McpPlusProperties.ThreadSafety threadSafety = properties.getContext().getThreadSafety();
            
            switch (threadSafety.getStrategy().toLowerCase()) {
                case "thread_local":
                    return new ThreadLocalContextHolder();
                case "inheritable":
                    return new InheritableThreadLocalContextHolder();
                case "adaptive":
                default:
                    return new AdaptiveContextHolder();
            }
        }
        
        @Bean
        @ConditionalOnMissingBean
        public McpPlusSessionManager mcpPlusSessionManager(McpPlusProperties properties) {
            McpPlusProperties.Session sessionConfig = properties.getContext().getSession();
            return new DefaultSessionManager(
                sessionConfig.getMaxInactiveInterval(),
                sessionConfig.isAutoCleanup()
            );
        }
        
        @Bean
        @ConditionalOnMissingBean
        public EnvironmentDetector environmentDetector() {
            return new EnvironmentDetector();
        }
    }
    
    /**
     * Simple ThreadLocal-based context holder.
     */
    static class ThreadLocalContextHolder implements McpPlusContextHolder {
        private final ThreadLocal<McpPlusContext> contextHolder = new ThreadLocal<>();
        
        @Override
        public void setContext(McpPlusContext context) {
            contextHolder.set(context);
        }
        
        @Override
        public McpPlusContext getContext() {
            McpPlusContext context = contextHolder.get();
            return context != null ? context : McpPlusContext.empty();
        }
        
        @Override
        public void clearContext() {
            contextHolder.remove();
        }
    }
    
    /**
     * InheritableThreadLocal-based context holder for async environments.
     */
    static class InheritableThreadLocalContextHolder implements McpPlusContextHolder {
        private final InheritableThreadLocal<McpPlusContext> contextHolder = new InheritableThreadLocal<>();
        
        @Override
        public void setContext(McpPlusContext context) {
            contextHolder.set(context);
        }
        
        @Override
        public McpPlusContext getContext() {
            McpPlusContext context = contextHolder.get();
            return context != null ? context : McpPlusContext.empty();
        }
        
        @Override
        public void clearContext() {
            contextHolder.remove();
        }
    }
}

