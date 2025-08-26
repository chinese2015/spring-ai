package com.example;

import org.springframework.ai.mcp.plus.core.context.*;
import org.springframework.ai.tool.annotation.Parameter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 完整的MCP ServerSession集成示例
 * 演示如何获取和使用McpServerSession
 */
@SpringBootApplication
public class McpPlusIntegrationExample {
    
    public static void main(String[] args) {
        SpringApplication.run(McpPlusIntegrationExample.class, args);
    }
}

/**
 * MCP Plus配置 - 演示如何集成McpServerSession
 */
@Configuration
class McpPlusIntegrationConfig {
    
    /**
     * 自定义传输提供者，拦截session创建
     * 这是获取McpServerSession的关键入口点
     */
    @Bean
    @Primary
    public MockMcpServerTransportProvider enhancedTransportProvider(
            McpPlusSessionManager sessionManager) {
        return new MockMcpServerTransportProvider(sessionManager);
    }
}

/**
 * 模拟的MCP传输提供者
 * 在实际应用中，这会是StdioServerTransportProvider的增强版本
 */
class MockMcpServerTransportProvider {
    
    private final McpPlusSessionManager sessionManager;
    private final Map<String, Object> activeMcpSessions = new ConcurrentHashMap<>();
    
    public MockMcpServerTransportProvider(McpPlusSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    /**
     * 模拟客户端连接事件
     * 在真实环境中，这会在MCP客户端连接时被调用
     */
    public void simulateClientConnection(String clientId) {
        // 1. 创建模拟的MCP server session
        MockMcpServerSession mcpSession = new MockMcpServerSession(clientId);
        
        // 2. 创建增强session，桥接协议层和应用层
        EnhancedMcpServerSession enhancedSession = 
            new EnhancedMcpServerSession(mcpSession);
        
        // 3. 注册到Plus session管理器
        sessionManager.saveSession(enhancedSession.getApplicationSession());
        activeMcpSessions.put(clientId, mcpSession);
        
        // 4. 设置消息处理器
        mcpSession.setMessageHandler(this::handleMcpMessage);
        
        System.out.println("MCP client connected: " + clientId);
        System.out.println("Enhanced session created with ID: " + 
                         enhancedSession.getApplicationSession().getId());
    }
    
    /**
     * 处理MCP消息的核心方法
     * 这里展示如何将McpServerSession桥接到MCP Plus上下文
     */
    private String handleMcpMessage(MockMcpServerSession mcpSession, String message) {
        // 创建包含MCP session信息的Plus上下文
        McpPlusContext context = McpPlusContextBridge.fromMcpServerSession(mcpSession);
        
        // 添加额外的上下文信息
        context.setAttribute("messageTimestamp", System.currentTimeMillis());
        context.setAttribute("clientId", mcpSession.getClientId());
        
        // 在Plus上下文中执行消息处理
        return McpPlusContextAccessor.runWithContext(context, () -> {
            // 在这个作用域内，所有工具都可以访问增强上下文
            return processMessage(message);
        });
    }
    
    private String processMessage(String message) {
        // 模拟消息处理逻辑
        // 在实际应用中，这里会调用Spring AI的工具
        return "Processed: " + message;
    }
    
    /**
     * 获取活跃的MCP session
     */
    public Optional<Object> getMcpSession(String clientId) {
        return Optional.ofNullable(activeMcpSessions.get(clientId));
    }
}

/**
 * 模拟的MCP Server Session
 * 在实际应用中，这是MCP Java SDK提供的真实session对象
 */
class MockMcpServerSession {
    
    private final String clientId;
    private final String sessionId;
    private MessageHandler messageHandler;
    
    public MockMcpServerSession(String clientId) {
        this.clientId = clientId;
        this.sessionId = "mcp-session-" + clientId;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }
    
    public String handleMessage(String message) {
        if (messageHandler != null) {
            return messageHandler.handle(this, message);
        }
        return "No handler set";
    }
    
    @FunctionalInterface
    interface MessageHandler {
        String handle(MockMcpServerSession session, String message);
    }
}

/**
 * 示例工具服务 - 演示如何在工具中使用MCP Plus上下文
 */
@Component
class McpIntegratedToolService {
    
    @Tool(name = "analyzeRequest", description = "Analyze request with full context")
    public AnalysisResult analyzeRequest(@Parameter("data") String data) {
        
        // 获取当前的Plus上下文
        McpPlusContext context = McpPlusContextAccessor.getCurrentContext();
        
        // 访问MCP协议层信息
        Optional<Object> mcpSession = context.getSession()
            .getUnderlyingMcpServerSession();
        
        String protocolInfo = "No MCP session";
        if (mcpSession.isPresent() && mcpSession.get() instanceof MockMcpServerSession mockSession) {
            protocolInfo = "MCP Client: " + mockSession.getClientId() + 
                          ", Session: " + mockSession.getSessionId();
        }
        
        // 访问Plus会话数据
        McpPlusSession session = context.getSession();
        
        // 获取之前的分析历史
        Integer analysisCount = session.getData("analysisCount", Integer.class)
            .orElse(0);
        analysisCount++;
        session.setData("analysisCount", analysisCount);
        
        // 访问请求元数据
        RequestMetadata metadata = context.getRequestMetadata();
        String transport = metadata.getTransportType();
        
        // 访问自定义属性
        Long timestamp = context.getAttribute("messageTimestamp", Long.class)
            .orElse(0L);
        String clientId = context.getAttribute("clientId", String.class)
            .orElse("unknown");
        
        return new AnalysisResult(
            data,
            protocolInfo,
            transport,
            analysisCount,
            timestamp,
            clientId,
            session.getId()
        );
    }
    
    @Tool(name = "sessionInfo", description = "Get current session information")
    public SessionInfo getSessionInfo() {
        McpPlusContext context = McpPlusContextAccessor.getCurrentContext();
        McpPlusSession session = context.getSession();
        
        return new SessionInfo(
            session.getId(),
            session.getCreatedAt().toString(),
            session.getLastAccessedAt().toString(),
            session.getAllData().size(),
            context.getRequestMetadata().getTransportType()
        );
    }
}

/**
 * 演示控制器 - 展示如何手动管理MCP session集成
 */
@Component
class McpIntegrationDemo {
    
    private final MockMcpServerTransportProvider transportProvider;
    private final McpIntegratedToolService toolService;
    
    public McpIntegrationDemo(MockMcpServerTransportProvider transportProvider,
                             McpIntegratedToolService toolService) {
        this.transportProvider = transportProvider;
        this.toolService = toolService;
    }
    
    /**
     * 演示完整的MCP客户端连接和消息处理流程
     */
    public void demonstrateIntegration() {
        System.out.println("=== MCP Plus Integration Demo ===");
        
        // 1. 模拟MCP客户端连接
        String clientId = "demo-client-001";
        transportProvider.simulateClientConnection(clientId);
        
        // 2. 获取MCP session并处理消息
        Optional<Object> mcpSession = transportProvider.getMcpSession(clientId);
        if (mcpSession.isPresent() && mcpSession.get() instanceof MockMcpServerSession mockSession) {
            
            // 3. 模拟多次消息处理，展示上下文传播
            processMessageWithContext(mockSession, "first analysis request");
            processMessageWithContext(mockSession, "second analysis request");
            
            // 4. 展示会话信息
            showSessionInfo(mockSession);
        }
    }
    
    private void processMessageWithContext(MockMcpServerSession mcpSession, String message) {
        System.out.println("\n--- Processing: " + message + " ---");
        
        // 创建Plus上下文
        McpPlusContext context = McpPlusContextBridge.fromMcpServerSession(mcpSession);
        context.setAttribute("messageTimestamp", System.currentTimeMillis());
        context.setAttribute("clientId", mcpSession.getClientId());
        
        // 在Plus上下文中执行工具
        AnalysisResult result = McpPlusContextAccessor.runWithContext(context, () -> {
            return toolService.analyzeRequest(message);
        });
        
        System.out.println("Analysis Result: " + result);
    }
    
    private void showSessionInfo(MockMcpServerSession mcpSession) {
        System.out.println("\n--- Session Information ---");
        
        McpPlusContext context = McpPlusContextBridge.fromMcpServerSession(mcpSession);
        
        SessionInfo info = McpPlusContextAccessor.runWithContext(context, () -> {
            return toolService.getSessionInfo();
        });
        
        System.out.println("Session Info: " + info);
    }
}

/**
 * 数据传输对象
 */
record AnalysisResult(
    String data,
    String protocolInfo,
    String transport,
    int analysisCount,
    long timestamp,
    String clientId,
    String sessionId
) {}

record SessionInfo(
    String sessionId,
    String createdAt,
    String lastAccessedAt,
    int dataSize,
    String transport
) {}
