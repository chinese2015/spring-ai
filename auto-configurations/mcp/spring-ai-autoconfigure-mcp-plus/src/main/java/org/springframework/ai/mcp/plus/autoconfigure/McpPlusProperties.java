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

package org.springframework.ai.mcp.plus.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(McpPlusProperties.CONFIG_PREFIX)
public class McpPlusProperties {

	public static final String CONFIG_PREFIX = "spring.ai.mcp.plus";

	private boolean enabled = true;

	/**
	 * Enable replacing default WebMVC transport provider with Plus provider.
	 */
	private boolean replaceWebMvcTransport = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isReplaceWebMvcTransport() {
		return replaceWebMvcTransport;
	}

	public void setReplaceWebMvcTransport(boolean replaceWebMvcTransport) {
		this.replaceWebMvcTransport = replaceWebMvcTransport;
	}

}
