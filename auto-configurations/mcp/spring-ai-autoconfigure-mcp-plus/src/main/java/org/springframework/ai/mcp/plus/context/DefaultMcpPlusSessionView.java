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

package org.springframework.ai.mcp.plus.context;

import java.util.Optional;

import io.modelcontextprotocol.server.McpSyncServerExchange;

/**
 * Default implementation backed by McpSyncServerExchange using only public APIs.
 */
public class DefaultMcpPlusSessionView implements McpPlusSessionView {

	private final McpSyncServerExchange exchange;

	public DefaultMcpPlusSessionView(McpSyncServerExchange exchange) {
		this.exchange = exchange;
	}

	@Override
	public String getId() {
		// McpSyncServerExchange implements equals/hashCode and can expose minimal
		// identity
		return Integer.toHexString(System.identityHashCode(this.exchange));
	}

	@Override
	public boolean isOpen() {
		// No public session state; conservatively return true while exchange is non-null
		return this.exchange != null;
	}

	@Override
	public Optional<Object> getUnderlying() {
		return Optional.of(this.exchange);
	}

}
