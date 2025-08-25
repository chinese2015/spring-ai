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

/**
 * Core context management functionality for Spring AI MCP Plus.
 * <p>
 * This package provides enhanced context capabilities for MCP Plus tools execution,
 * including:
 * <ul>
 * <li>Rich context access through {@link org.springframework.ai.mcp.plus.core.context.McpPlusContext}</li>
 * <li>Session management with {@link org.springframework.ai.mcp.plus.core.context.McpPlusSession}</li>
 * <li>Adaptive context propagation via {@link org.springframework.ai.mcp.plus.core.context.AdaptiveContextHolder}</li>
 * <li>Environment detection through {@link org.springframework.ai.mcp.plus.core.context.EnvironmentDetector}</li>
 * <li>Convenient static access using {@link org.springframework.ai.mcp.plus.core.context.McpPlusContextAccessor}</li>
 * </ul>
 * <p>
 * The context system is designed to be:
 * <ul>
 * <li><strong>Thread-safe</strong> - Works correctly in multi-threaded environments</li>
 * <li><strong>Environment-aware</strong> - Automatically adapts to Servlet, Reactive, or Async environments</li>
 * <li><strong>Non-intrusive</strong> - Extends Spring AI capabilities without modifying existing code</li>
 * <li><strong>Spring-native</strong> - Integrates seamlessly with Spring Boot and Spring Framework</li>
 * </ul>
 * 
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Get current context
 * McpPlusContext context = McpPlusContextAccessor.getCurrentContext();
 * 
 * // Work with session
 * McpPlusSession session = context.getSession();
 * session.setData("key", "value");
 * Optional<String> value = session.getData("key", String.class);
 * 
 * // Access request metadata
 * RequestMetadata metadata = context.getRequestMetadata();
 * String transport = metadata.getTransportType();
 * 
 * // Handle authentication
 * Optional<String> token = context.getAuthToken();
 * }</pre>
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
@NonNullApi
@NonNullFields
package org.springframework.ai.mcp.plus.core.context;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
