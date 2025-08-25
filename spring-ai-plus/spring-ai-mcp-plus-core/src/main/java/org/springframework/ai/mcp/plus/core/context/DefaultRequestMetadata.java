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

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of RequestMetadata.
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
public class DefaultRequestMetadata implements RequestMetadata {
    
    private final String requestId;
    private final Instant timestamp;
    private final String transportType;
    private final String clientInfo;
    private final String remoteAddress;
    private final Map<String, Object> additionalMetadata;
    
    /**
     * Create a new DefaultRequestMetadata with minimal information.
     * 
     * @param transportType the transport type
     */
    public DefaultRequestMetadata(String transportType) {
        this(null, Instant.now(), transportType, null, null, Collections.emptyMap());
    }
    
    /**
     * Create a new DefaultRequestMetadata with full information.
     * 
     * @param requestId the request ID
     * @param timestamp the request timestamp
     * @param transportType the transport type
     * @param clientInfo the client information
     * @param remoteAddress the remote address
     * @param additionalMetadata additional metadata
     */
    public DefaultRequestMetadata(String requestId, Instant timestamp, String transportType,
                                 String clientInfo, String remoteAddress, 
                                 Map<String, Object> additionalMetadata) {
        this.requestId = requestId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.transportType = transportType != null ? transportType : "UNKNOWN";
        this.clientInfo = clientInfo;
        this.remoteAddress = remoteAddress;
        this.additionalMetadata = Collections.unmodifiableMap(
            additionalMetadata != null ? new HashMap<>(additionalMetadata) : Collections.emptyMap()
        );
    }
    
    @Override
    public Optional<String> getRequestId() {
        return Optional.ofNullable(requestId);
    }
    
    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String getTransportType() {
        return transportType;
    }
    
    @Override
    public Optional<String> getClientInfo() {
        return Optional.ofNullable(clientInfo);
    }
    
    @Override
    public Optional<String> getRemoteAddress() {
        return Optional.ofNullable(remoteAddress);
    }
    
    @Override
    public Map<String, Object> getAdditionalMetadata() {
        return additionalMetadata;
    }
    
    @Override
    public Optional<Object> getMetadata(String key) {
        return Optional.ofNullable(additionalMetadata.get(key));
    }
    
    /**
     * Builder for creating DefaultRequestMetadata instances.
     */
    public static class Builder {
        private String requestId;
        private Instant timestamp;
        private String transportType = "UNKNOWN";
        private String clientInfo;
        private String remoteAddress;
        private Map<String, Object> additionalMetadata = new HashMap<>();
        
        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder transportType(String transportType) {
            this.transportType = transportType;
            return this;
        }
        
        public Builder clientInfo(String clientInfo) {
            this.clientInfo = clientInfo;
            return this;
        }
        
        public Builder remoteAddress(String remoteAddress) {
            this.remoteAddress = remoteAddress;
            return this;
        }
        
        public Builder addMetadata(String key, Object value) {
            this.additionalMetadata.put(key, value);
            return this;
        }
        
        public Builder additionalMetadata(Map<String, Object> metadata) {
            this.additionalMetadata.clear();
            if (metadata != null) {
                this.additionalMetadata.putAll(metadata);
            }
            return this;
        }
        
        public DefaultRequestMetadata build() {
            return new DefaultRequestMetadata(requestId, timestamp, transportType, 
                                            clientInfo, remoteAddress, additionalMetadata);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
