/*
 * Copyright 2025-2025 the original author or authors.
 */
package org.springframework.ai.mcp.plus.transport;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.ai.mcp.plus.context.SessionThreadLocalContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.function.ServerResponse.SseBuilder;

/**
 * WebMVC SSE TransportProvider with ThreadLocal session propagation.
 *
 * It mirrors the SDK's WebMvcSseServerTransportProvider behavior and, in addition, sets
 * the McpServerSession into a ThreadLocal around message handling to enable context
 * propagation into tool execution.
 */
public class WebMvcSseServerTransportProviderPlus implements McpServerTransportProvider {

	private static final Logger logger = LoggerFactory.getLogger(WebMvcSseServerTransportProviderPlus.class);

	public static final String MESSAGE_EVENT_TYPE = "message";

	public static final String ENDPOINT_EVENT_TYPE = "endpoint";

	public static final String DEFAULT_SSE_ENDPOINT = "/sse";

	private final ObjectMapper objectMapper;

	private final String messageEndpoint;

	private final String sseEndpoint;

	private final String baseUrl;

	private final RouterFunction<ServerResponse> routerFunction;

	private McpServerSession.Factory sessionFactory;

	private final ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap<>();

	private volatile boolean isClosing = false;

	public WebMvcSseServerTransportProviderPlus(ObjectMapper objectMapper, String messageEndpoint) {
		this(objectMapper, messageEndpoint, DEFAULT_SSE_ENDPOINT);
	}

	public WebMvcSseServerTransportProviderPlus(ObjectMapper objectMapper, String messageEndpoint, String sseEndpoint) {
		this(objectMapper, "", messageEndpoint, sseEndpoint);
	}

	public WebMvcSseServerTransportProviderPlus(ObjectMapper objectMapper, String baseUrl, String messageEndpoint,
			String sseEndpoint) {
		this.objectMapper = objectMapper;
		this.baseUrl = baseUrl;
		this.messageEndpoint = messageEndpoint;
		this.sseEndpoint = sseEndpoint;
		this.routerFunction = RouterFunctions.route()
			.GET(this.sseEndpoint, this::handleSseConnection)
			.POST(this.messageEndpoint, this::handleMessage)
			.build();
	}

	@Override
	public void setSessionFactory(McpServerSession.Factory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public Mono<Void> notifyClients(String method, Object params) {
		if (sessions.isEmpty()) {
			logger.debug("No active sessions to broadcast message to");
			return Mono.empty();
		}

		logger.debug("Attempting to broadcast message to {} active sessions", sessions.size());

		return Flux.fromIterable(sessions.values())
			.flatMap(session -> session.sendNotification(method, params)
				.doOnError(
						e -> logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage()))
				.onErrorComplete())
			.then();
	}

	@Override
	public Mono<Void> closeGracefully() {
		return Flux.fromIterable(sessions.values()).doFirst(() -> {
			this.isClosing = true;
			logger.debug("Initiating graceful shutdown with {} active sessions", sessions.size());
		})
			.flatMap(McpServerSession::closeGracefully)
			.then()
			.doOnSuccess(v -> logger.debug("Graceful shutdown completed"));
	}

	public RouterFunction<ServerResponse> getRouterFunction() {
		return this.routerFunction;
	}

	private ServerResponse handleSseConnection(ServerRequest request) {
		if (this.isClosing) {
			return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
		}

		String sessionId = UUID.randomUUID().toString();
		logger.debug("Creating new SSE connection for session: {}", sessionId);

		try {
			return ServerResponse.sse(sseBuilder -> {
				sseBuilder.onComplete(() -> {
					logger.debug("SSE connection completed for session: {}", sessionId);
					sessions.remove(sessionId);
				});
				sseBuilder.onTimeout(() -> {
					logger.debug("SSE connection timed out for session: {}", sessionId);
					sessions.remove(sessionId);
				});

				WebMvcMcpSessionTransport sessionTransport = new WebMvcMcpSessionTransport(sessionId, sseBuilder);
				McpServerSession session = sessionFactory.create(sessionTransport);
				this.sessions.put(sessionId, session);

				try {
					sseBuilder.id(sessionId)
						.event(ENDPOINT_EVENT_TYPE)
						.data(this.baseUrl + this.messageEndpoint + "?sessionId=" + sessionId);
				}
				catch (Exception e) {
					logger.error("Failed to send initial endpoint event: {}", e.getMessage());
					sseBuilder.error(e);
				}
			}, Duration.ZERO);
		}
		catch (Exception e) {
			logger.error("Failed to send initial endpoint event to session {}: {}", sessionId, e.getMessage());
			sessions.remove(sessionId);
			return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	private ServerResponse handleMessage(ServerRequest request) {
		if (this.isClosing) {
			return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
		}

		if (request.param("sessionId").isEmpty()) {
			return ServerResponse.badRequest().body(new McpError("Session ID missing in message endpoint"));
		}

		String sessionId = request.param("sessionId").get();
		McpServerSession session = sessions.get(sessionId);

		if (session == null) {
			return ServerResponse.status(HttpStatus.NOT_FOUND).body(new McpError("Session not found: " + sessionId));
		}

		try {
			String body = request.body(String.class);
			McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body);

			// ThreadLocal set/clear around message handling to propagate session
			SessionThreadLocalContext.set(session);
			try {
				session.handle(message).block();
			}
			finally {
				SessionThreadLocalContext.clear();
			}

			return ServerResponse.ok().build();
		}
		catch (IllegalArgumentException | IOException e) {
			logger.error("Failed to deserialize message: {}", e.getMessage());
			return ServerResponse.badRequest().body(new McpError("Invalid message format"));
		}
		catch (Exception e) {
			logger.error("Error handling message: {}", e.getMessage());
			return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new McpError(e.getMessage()));
		}
	}

	private class WebMvcMcpSessionTransport implements McpServerTransport {

		private final String sessionId;

		private final SseBuilder sseBuilder;

		WebMvcMcpSessionTransport(String sessionId, SseBuilder sseBuilder) {
			this.sessionId = sessionId;
			this.sseBuilder = sseBuilder;
			logger.debug("Session transport {} initialized with SSE builder", sessionId);
		}

		@Override
		public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
			return Mono.fromRunnable(() -> {
				try {
					String jsonText = objectMapper.writeValueAsString(message);
					sseBuilder.id(sessionId).event(MESSAGE_EVENT_TYPE).data(jsonText);
					logger.debug("Message sent to session {}", sessionId);
				}
				catch (Exception e) {
					logger.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
					sseBuilder.error(e);
				}
			});
		}

		@Override
		public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
			return objectMapper.convertValue(data, typeRef);
		}

		@Override
		public Mono<Void> closeGracefully() {
			return Mono.fromRunnable(() -> {
				logger.debug("Closing session transport: {}", sessionId);
				try {
					sseBuilder.complete();
					logger.debug("Successfully completed SSE builder for session {}", sessionId);
				}
				catch (Exception e) {
					logger.warn("Failed to complete SSE builder for session {}: {}", sessionId, e.getMessage());
				}
			});
		}

		@Override
		public void close() {
			try {
				sseBuilder.complete();
				logger.debug("Successfully completed SSE builder for session {}", sessionId);
			}
			catch (Exception e) {
				logger.warn("Failed to complete SSE builder for session {}: {}", sessionId, e.getMessage());
			}
		}

	}

}
