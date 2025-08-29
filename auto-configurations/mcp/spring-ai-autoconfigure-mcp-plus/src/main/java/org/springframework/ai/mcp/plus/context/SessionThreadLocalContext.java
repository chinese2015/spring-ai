/*
 * Copyright 2025-2025 the original author or authors.
 */
package org.springframework.ai.mcp.plus.context;

import java.util.Optional;

import io.modelcontextprotocol.spec.McpServerSession;

public final class SessionThreadLocalContext {

	private static final ThreadLocal<McpServerSession> SESSION_HOLDER = new ThreadLocal<>();

	private SessionThreadLocalContext() {
	}

	public static void set(McpServerSession session) {
		SESSION_HOLDER.set(session);
	}

	public static Optional<McpServerSession> get() {
		return Optional.ofNullable(SESSION_HOLDER.get());
	}

	public static void clear() {
		SESSION_HOLDER.remove();
	}

}
