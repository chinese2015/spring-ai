/*
 * Copyright 2025-2025 the original author or authors.
 */
package org.springframework.ai.mcp.plus.enricher;

import java.util.Map;
import java.util.Optional;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.plus.context.SessionThreadLocalContext;

import io.modelcontextprotocol.spec.McpServerSession;

public class ServerSessionEnricher implements ToolContextEnricher {

	public static final String KEY = "spring.ai.mcp.plus.serverSession";

	@Override
	public boolean supports(ToolContext ctx) {
		return true;
	}

	@Override
	public Map<String, Object> enrich(ToolContext ctx) {
		Optional<McpServerSession> session = SessionThreadLocalContext.get();
		return session.<Map<String, Object>>map(s -> Map.of(KEY, s)).orElseGet(Map::of);
	}

}
