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
 * Non-intrusive integration components for Spring AI MCP Plus.
 * <p>
 * This package provides the core integration strategy that allows MCP Plus
 * to enhance existing Spring AI MCP functionality without modifying the
 * original transport providers or session management.
 * <p>
 * Key components:
 * <ul>
 * <li>{@link org.springframework.ai.mcp.plus.core.integration.McpSessionRegistrar} - 
 *     Non-intrusive session wrapper and registry</li>
 * <li>{@link org.springframework.ai.mcp.plus.core.integration.McpPlusToolAspect} - 
 *     AOP aspect for automatic tool enhancement</li>
 * </ul>
 * <p>
 * Integration Strategy:
 * <ul>
 * <li><strong>Non-intrusive</strong> - Works with existing Spring AI transport providers</li>
 * <li><strong>Session Discovery</strong> - Multiple strategies to find current MCP sessions</li>
 * <li><strong>Automatic Enhancement</strong> - Tools get MCP Plus context transparently</li>
 * <li><strong>Thread-safe</strong> - Proper concurrent access and cleanup</li>
 * </ul>
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
@NonNullApi
@NonNullFields
package org.springframework.ai.mcp.plus.core.integration;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
