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

package org.springframework.ai.mcp.plus.enricher;

import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.plus.context.DefaultMcpPlusSessionView;
import org.springframework.ai.mcp.plus.context.McpPlusSessionView;

/**
 * Builds a minimal session view from MCP exchange and adds it under namespaced key.
 */
@Deprecated
public class McpPlusSessionEnricher implements ToolContextEnricher {

	public static final String PLUS_SESSION_KEY = "spring.ai.mcp.plus.session";

	@Override
	public boolean supports(ToolContext ctx) {
		return false;
	}

	@Override
	public Map<String, Object> enrich(ToolContext ctx) {
		return Map.of();
	}

	private McpPlusSessionView buildSessionView(io.modelcontextprotocol.server.McpSyncServerExchange exchange) {
		return new DefaultMcpPlusSessionView(exchange);
	}

}
