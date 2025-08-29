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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.plus.context.SessionThreadLocalAccessor;
import org.springframework.ai.mcp.plus.decorate.McpPlusToolCallbackDecorator;
import org.springframework.ai.mcp.plus.enricher.ServerSessionEnricher;
import org.springframework.ai.mcp.plus.enricher.ToolContextEnricher;
import org.springframework.ai.mcp.plus.transport.WebMvcSseServerTransportProviderPlus;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;

@AutoConfiguration
@ConditionalOnClass({ ToolContext.class, ToolCallback.class, McpToolUtils.class })
@EnableConfigurationProperties(McpPlusProperties.class)
@ConditionalOnProperty(prefix = McpPlusProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class McpPlusAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public List<ToolContextEnricher> mcpPlusDefaultEnrichers() {
		List<ToolContextEnricher> enrichers = new ArrayList<>();
		enrichers.add(new ServerSessionEnricher());
		return enrichers;
	}

	@jakarta.annotation.PostConstruct
	public void initContextPropagation() {
		io.micrometer.context.ContextRegistry.getInstance()
			.registerThreadLocalAccessor(new SessionThreadLocalAccessor());
		reactor.core.publisher.Hooks.enableAutomaticContextPropagation();
	}

	@Bean
	@ConditionalOnMissingBean
	public Function<ToolCallback, ToolCallback> mcpPlusToolCallbackWrapper(List<ToolContextEnricher> enrichers) {
		return delegate -> new McpPlusToolCallbackDecorator(delegate, enrichers);
	}

	@Bean
	@ConditionalOnMissingBean
	public McpPlusToolBeanPostProcessor mcpPlusToolBeanPostProcessor(Function<ToolCallback, ToolCallback> wrapper) {
		return new McpPlusToolBeanPostProcessor(wrapper);
	}

	// Replace WebMVC transport provider when desired
	@Bean
	@Primary
	@ConditionalOnClass(name = "io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider")
	@ConditionalOnProperty(prefix = McpPlusProperties.CONFIG_PREFIX, name = "replaceWebMvcTransport",
			havingValue = "true", matchIfMissing = true)
	public WebMvcSseServerTransportProviderPlus webMvcSseServerTransportProviderPlus(
			ObjectProvider<ObjectMapper> objectMapperProvider,
			org.springframework.ai.mcp.server.autoconfigure.McpServerProperties serverProperties) {
		ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
		return new WebMvcSseServerTransportProviderPlus(objectMapper, serverProperties.getBaseUrl(),
				serverProperties.getSseMessageEndpoint(), serverProperties.getSseEndpoint());
	}

	@Bean
	@ConditionalOnClass(name = "org.springframework.web.servlet.function.RouterFunction")
	@ConditionalOnProperty(prefix = McpPlusProperties.CONFIG_PREFIX, name = "replaceWebMvcTransport",
			havingValue = "true", matchIfMissing = true)
	public org.springframework.web.servlet.function.RouterFunction<org.springframework.web.servlet.function.ServerResponse> mvcMcpRouterFunctionPlus(
			WebMvcSseServerTransportProviderPlus transportProvider) {
		return transportProvider.getRouterFunction();
	}

	static class McpPlusToolBeanPostProcessor implements org.springframework.beans.factory.config.BeanPostProcessor {

		private final Function<ToolCallback, ToolCallback> wrapper;

		McpPlusToolBeanPostProcessor(Function<ToolCallback, ToolCallback> wrapper) {
			this.wrapper = wrapper;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (bean instanceof ToolCallback callback) {
				if (callback instanceof McpPlusToolCallbackDecorator) {
					return bean;
				}
				return this.wrapper.apply(callback);
			}

			if (bean instanceof ToolCallbackProvider provider) {
				return (ToolCallbackProvider) () -> {
					ToolCallback[] callbacks = provider.getToolCallbacks();
					ToolCallback[] wrapped = new ToolCallback[callbacks.length];
					for (int i = 0; i < callbacks.length; i++) {
						wrapped[i] = (callbacks[i] instanceof McpPlusToolCallbackDecorator) ? callbacks[i]
								: this.wrapper.apply(callbacks[i]);
					}
					return wrapped;
				};
			}

			if (bean instanceof ToolCallbackResolver resolver) {
				return (ToolCallbackResolver) toolName -> {
					ToolCallback resolved = resolver.resolve(toolName);
					if (resolved == null) {
						return null;
					}
					return (resolved instanceof McpPlusToolCallbackDecorator) ? resolved : this.wrapper.apply(resolved);
				};
			}

			return bean;
		}

	}

}
