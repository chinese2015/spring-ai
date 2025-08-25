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
import java.util.Map;
import java.util.Optional;

/**
 * Contains metadata about the current request being processed.
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
public interface RequestMetadata {
    
    /**
     * Get the request ID.
     * 
     * @return the request ID if available
     */
    Optional<String> getRequestId();
    
    /**
     * Get the timestamp when the request was received.
     * 
     * @return the request timestamp
     */
    Instant getTimestamp();
    
    /**
     * Get the transport type used for this request.
     * 
     * @return the transport type (e.g., "HTTP", "STDIO", "WEBSOCKET")
     */
    String getTransportType();
    
    /**
     * Get the client information.
     * 
     * @return the client information if available
     */
    Optional<String> getClientInfo();
    
    /**
     * Get the remote address/endpoint.
     * 
     * @return the remote address if available
     */
    Optional<String> getRemoteAddress();
    
    /**
     * Get additional metadata as key-value pairs.
     * 
     * @return additional metadata
     */
    Map<String, Object> getAdditionalMetadata();
    
    /**
     * Get a specific metadata value.
     * 
     * @param key the metadata key
     * @return the metadata value if present
     */
    Optional<Object> getMetadata(String key);
}
