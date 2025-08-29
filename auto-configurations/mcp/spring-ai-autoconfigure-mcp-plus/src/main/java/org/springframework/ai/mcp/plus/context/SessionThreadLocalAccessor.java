/*
 * Copyright 2025-2025 the original author or authors.
 */
package org.springframework.ai.mcp.plus.context;

import java.util.Optional;

import io.micrometer.context.ThreadLocalAccessor;
import io.modelcontextprotocol.spec.McpServerSession;

public class SessionThreadLocalAccessor implements ThreadLocalAccessor {

	public static final String KEY = "spring.ai.mcp.plus.serverSession";

	@Override
	public Object key() {
		return KEY;
	}

	@Override
	public Object getValue() {
		Optional<McpServerSession> session = SessionThreadLocalContext.get();
		return session.orElse(null);
	}

	@Override
	public void setValue(Object value) {
		if (value instanceof McpServerSession s) {
			SessionThreadLocalContext.set(s);
		}
	}

	@Override
	public void reset() {
		SessionThreadLocalContext.clear();
	}

}
