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

package org.springframework.ai.mcp.plus.decorate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.plus.enricher.ToolContextEnricher;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;

/**
 * Decorates a ToolCallback to perform a replacement-merge on the ToolContext before
 * delegating, only impacting methods that accept ToolContext.
 */
public class McpPlusToolCallbackDecorator implements ToolCallback {

	private final ToolCallback delegate;

	private final List<ToolContextEnricher> enrichers;

	public McpPlusToolCallbackDecorator(ToolCallback delegate, List<ToolContextEnricher> enrichers) {
		this.delegate = delegate;
		this.enrichers = (enrichers != null) ? enrichers : new ArrayList<>();
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return this.delegate.getToolDefinition();
	}

	@Override
	public ToolMetadata getToolMetadata() {
		return this.delegate.getToolMetadata();
	}

	@Override
	public String call(String toolInput) {
		return this.delegate.call(toolInput);
	}

	@Override
	public String call(String toolInput, @Nullable ToolContext toolContext) {
		ToolContext enriched = buildEnrichedToolContext(toolContext);
		try {
			return this.delegate.call(toolInput, enriched);
		}
		catch (UnsupportedOperationException ex) {
			// delegate does not support non-empty context → gracefully downgrade
			return this.delegate.call(toolInput);
		}
	}

	private ToolContext buildEnrichedToolContext(@Nullable ToolContext original) {
		Map<String, Object> merged = new HashMap<>();
		ToolContext safeOriginal = (original != null) ? original : new ToolContext(Map.of());

		if (safeOriginal.getContext() != null && !safeOriginal.getContext().isEmpty()) {
			merged.putAll(safeOriginal.getContext());
		}

		for (ToolContextEnricher enricher : this.enrichers) {
			try {
				if (enricher.supports(safeOriginal)) {
					Map<String, Object> additions = enricher.enrich(safeOriginal);
					if (additions != null && !additions.isEmpty()) {
						merged.putAll(additions);
					}
				}
			}
			catch (Exception ignored) {
				// best-effort; a failing enricher must not block others
			}
		}

		if (merged.isEmpty()) {
			// Ensure non-empty context for methods that require ToolContext as parameter
			merged.put("spring.ai.mcp.plus.enabled", Boolean.TRUE);
		}

		return new ToolContext(merged);
	}

}
