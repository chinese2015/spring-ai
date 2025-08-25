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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for Spring AI MCP Plus.
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.ai.mcp.plus")
public class McpPlusProperties {
    
    /**
     * Whether MCP Plus is enabled.
     */
    private boolean enabled = true;
    
    /**
     * Context configuration.
     */
    private Context context = new Context();
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Context getContext() {
        return context;
    }
    
    public void setContext(Context context) {
        this.context = context;
    }
    
    /**
     * Context-related configuration.
     */
    public static class Context {
        
        /**
         * Whether context features are enabled.
         */
        private boolean enabled = true;
        
        /**
         * Session configuration.
         */
        private Session session = new Session();
        
        /**
         * Thread safety configuration.
         */
        private ThreadSafety threadSafety = new ThreadSafety();
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public Session getSession() {
            return session;
        }
        
        public void setSession(Session session) {
            this.session = session;
        }
        
        public ThreadSafety getThreadSafety() {
            return threadSafety;
        }
        
        public void setThreadSafety(ThreadSafety threadSafety) {
            this.threadSafety = threadSafety;
        }
    }
    
    /**
     * Session-related configuration.
     */
    public static class Session {
        
        /**
         * Maximum inactive interval for sessions.
         */
        private Duration maxInactiveInterval = Duration.ofMinutes(30);
        
        /**
         * Whether to enable persistent sessions (not implemented yet).
         */
        private boolean persistentSessions = false;
        
        /**
         * Storage type for sessions (memory, redis, database - only memory is implemented).
         */
        private String storageType = "memory";
        
        /**
         * Whether to automatically cleanup expired sessions.
         */
        private boolean autoCleanup = true;
        
        public Duration getMaxInactiveInterval() {
            return maxInactiveInterval;
        }
        
        public void setMaxInactiveInterval(Duration maxInactiveInterval) {
            this.maxInactiveInterval = maxInactiveInterval;
        }
        
        public boolean isPersistentSessions() {
            return persistentSessions;
        }
        
        public void setPersistentSessions(boolean persistentSessions) {
            this.persistentSessions = persistentSessions;
        }
        
        public String getStorageType() {
            return storageType;
        }
        
        public void setStorageType(String storageType) {
            this.storageType = storageType;
        }
        
        public boolean isAutoCleanup() {
            return autoCleanup;
        }
        
        public void setAutoCleanup(boolean autoCleanup) {
            this.autoCleanup = autoCleanup;
        }
    }
    
    /**
     * Thread safety configuration.
     */
    public static class ThreadSafety {
        
        /**
         * Context propagation strategy (thread_local, inheritable, adaptive).
         */
        private String strategy = "adaptive";
        
        /**
         * Whether to use InheritableThreadLocal for async environments.
         */
        private boolean inheritableThreadLocal = true;
        
        public String getStrategy() {
            return strategy;
        }
        
        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }
        
        public boolean isInheritableThreadLocal() {
            return inheritableThreadLocal;
        }
        
        public void setInheritableThreadLocal(boolean inheritableThreadLocal) {
            this.inheritableThreadLocal = inheritableThreadLocal;
        }
    }
}
