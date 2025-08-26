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

import java.util.Collections;
import java.util.Map;

/**
 * Temporary implementation of ToolContext for demonstration purposes.
 * In real usage, this would be replaced by org.springframework.ai.chat.model.ToolContext
 * 
 * @author Spring AI MCP Plus Team
 * @since 1.0.0
 */
public final class ToolContext {
    
    private final Map<String, Object> context;
    
    /**
     * Constructs a new ToolContext with the given context map.
     * @param context A map containing the tool context information.
     */
    public ToolContext(Map<String, Object> context) {
        this.context = Collections.unmodifiableMap(context);
    }
    
    /**
     * Returns the immutable context map.
     * @return An unmodifiable view of the context map.
     */
    public Map<String, Object> getContext() {
        return this.context;
    }
}

