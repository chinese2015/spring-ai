# MCP Plus Context 集成示例

## 概述

本文档展示了如何正确地扩展 MCP Java SDK 的 `McpServerSession` 并增强 Spring AI 的 `ToolContext`，实现三层架构的无缝集成。

## 架构层次

```
┌─────────────────────────────────────────────────────────────┐
│                    MCP Plus 应用层                           │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │  McpPlusContext │  │ McpPlusSession  │  │ RequestMetadata │ │
│  │                 │  │                 │  │                 │ │
│  │ - 会话管理       │  │ - 业务数据存储   │  │ - 请求元信息     │ │
│  │ - 认证信息       │  │ - 生命周期管理   │  │ - Transport类型  │ │
│  │ - 自定义属性     │  │ - 线程安全      │  │ - 客户端信息     │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                    Spring AI 工具执行层                      │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                   ToolContext                           │ │
│  │                                                         │ │
│  │ - Map<String, Object> context                          │ │
│  │ - Tool call history                                    │ │
│  │ - 不可变上下文传递                                       │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                     MCP Java SDK 协议层                     │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                 McpServerSession                        │ │
│  │                                                         │ │
│  │ - JSON-RPC 消息处理                                     │ │
│  │ - Transport 管理 (stdio/http/ws)                       │ │
│  │ - 协议状态管理                                          │ │
│  │ - 客户端连接管理                                        │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 实际集成代码示例

### 1. 扩展 MCP Server Session

```java
// 扩展 MCP Java SDK 的 McpServerSession
public class EnhancedMcpServerSession {
    private final Object mcpServerSession;        // 协议层：MCP Java SDK session
    private final McpPlusSession applicationSession; // 应用层：业务 session
    
    public EnhancedMcpServerSession(Object mcpServerSession) {
        this.mcpServerSession = mcpServerSession;
        this.applicationSession = new DefaultMcpPlusSession();
    }
    
    // 协议层操作
    public Object getMcpServerSession() {
        return mcpServerSession;
    }
    
    // 应用层操作  
    public McpPlusSession getApplicationSession() {
        return applicationSession;
    }
}
```

### 2. 上下文桥接

```java
// 桥接不同层次的上下文
public class McpPlusContextBridge {
    
    // 从 Spring AI ToolContext 创建增强上下文
    public static McpPlusContext fromToolContext(ToolContext toolContext) {
        return new DefaultMcpPlusContext(toolContext);
    }
    
    // 从 MCP Server Session 创建增强上下文
    public static McpPlusContext fromMcpServerSession(Object mcpServerSession) {
        EnhancedMcpServerSession enhanced = new EnhancedMcpServerSession(mcpServerSession);
        return new DefaultMcpPlusContext(null, enhanced.getApplicationSession(), 
                                       new DefaultRequestMetadata("MCP"), Map.of(), Map.of());
    }
    
    // 综合两个层次创建完整上下文
    public static McpPlusContext fromBothContexts(ToolContext toolContext, Object mcpServerSession) {
        EnhancedMcpServerSession enhanced = new EnhancedMcpServerSession(mcpServerSession);
        return new DefaultMcpPlusContext(toolContext, enhanced.getApplicationSession(),
                                       new DefaultRequestMetadata("MCP"), Map.of(), 
                                       toolContext != null ? toolContext.getContext() : Map.of());
    }
}
```

### 3. 实际使用场景

#### 场景 A：纯 Spring AI 工具（向后兼容）

```java
@Component
public class LegacyUserService {
    
    @Tool(name = "getUser", description = "Get user by ID")
    public User getUser(String userId, ToolContext context) {
        // 现有工具无需修改，继续正常工作
        List<Message> history = context.getToolCallHistory();
        return userRepository.findById(userId);
    }
}
```

#### 场景 B：增强工具（使用 MCP Plus 功能）

```java
@Component
public class EnhancedUserService {
    
    @Tool(name = "getUserWithAuth", description = "Get user with authentication")
    public User getUserWithAuth(String userId) {
        // 通过静态访问器获取增强上下文
        McpPlusContext context = McpPlusContextAccessor.getCurrentContext();
        
        // 访问认证信息
        String authToken = context.getAuthToken().orElseThrow(
            () -> new SecurityException("Authentication required")
        );
        
        // 访问会话数据
        McpPlusSession session = context.getSession();
        Optional<UserPreferences> prefs = session.getData("preferences", UserPreferences.class);
        
        // 访问请求元数据
        RequestMetadata metadata = context.getRequestMetadata();
        String clientInfo = metadata.getClientUserAgent().orElse("Unknown");
        
        // 也可以访问原始 Spring AI ToolContext（如果存在）
        ToolContext toolContext = context.getToolContext();
        
        return userRepository.findByIdWithAuth(userId, authToken, prefs.orElse(null));
    }
}
```

#### 场景 C：MCP Server Session 集成

```java
@Component
public class McpIntegratedService {
    
    public void handleMcpRequest(Object mcpServerSession, String toolInput) {
        // 从 MCP Server Session 创建增强上下文
        McpPlusContext context = McpPlusContextBridge.fromMcpServerSession(mcpServerSession);
        
        // 设置上下文并执行工具
        McpPlusContextAccessor.runWithContext(context, () -> {
            // 在这个块内，所有工具都可以访问增强上下文
            ToolCallback tool = toolRegistry.findTool("getUserWithAuth");
            String result = tool.call(toolInput);
            
            // 处理结果...
        });
    }
}
```

#### 场景 D：完整集成（同时有 ToolContext 和 McpServerSession）

```java
@Component
public class FullIntegrationService {
    
    public String executeToolWithFullContext(String toolName, String toolInput, 
                                           ToolContext toolContext, Object mcpServerSession) {
        
        // 创建综合上下文
        McpPlusContext fullContext = McpPlusContextBridge.fromBothContexts(toolContext, mcpServerSession);
        
        // 执行工具
        return McpPlusContextAccessor.runWithContext(fullContext, () -> {
            ToolCallback tool = toolRegistry.findTool(toolName);
            return tool.call(toolInput, toolContext); // 保持向后兼容
        });
    }
}
```

### 4. 中间件集成示例

```java
@Component
public class AuthenticatedToolService {
    
    @Tool(name = "secureOperation", description = "Secure operation requiring auth")
    @Middleware({AuthenticationInterceptor.class, AuditLogInterceptor.class})
    public OperationResult secureOperation(String operationId) {
        
        McpPlusContext context = McpPlusContextAccessor.getCurrentContext();
        
        // 中间件已经验证了认证，我们可以安全地获取用户信息
        UserInfo user = context.getAttribute("currentUser", UserInfo.class)
            .orElseThrow(() -> new SecurityException("User not authenticated"));
        
        // 从会话获取操作历史
        McpPlusSession session = context.getSession();
        List<String> operationHistory = session.getData("operationHistory", List.class)
            .orElse(new ArrayList<>());
        
        // 执行安全操作
        OperationResult result = performSecureOperation(operationId, user);
        
        // 更新操作历史
        operationHistory.add(operationId);
        session.setData("operationHistory", operationHistory);
        
        return result;
    }
    
    private OperationResult performSecureOperation(String operationId, UserInfo user) {
        // 实际的业务逻辑
        return new OperationResult(operationId, "Success", user.getId());
    }
}
```

### 5. 配置示例

```yaml
spring:
  ai:
    mcp:
      plus:
        enabled: true
        context:
          enabled: true
          session:
            max-inactive-interval: PT30M
            auto-cleanup: true
          thread-safety:
            strategy: adaptive  # 自动检测环境并选择合适的策略
            inheritable-thread-local: true
        middleware:
          enabled: true
          global-interceptors:
            - authenticationInterceptor
            - loggingInterceptor
```

## 关键设计优势

### 1. **清晰的职责分离**
- **协议层**：MCP Java SDK 处理通信协议
- **工具层**：Spring AI 处理工具执行
- **应用层**：MCP Plus 处理业务逻辑

### 2. **完全向后兼容**
- 现有 Spring AI 工具无需修改
- 可以渐进式采用新功能
- 支持混合使用场景

### 3. **灵活的集成选项**
- 只使用 Spring AI 增强
- 只使用 MCP Session 集成
- 同时使用两者获得完整功能

### 4. **线程安全的上下文传播**
- 自动检测运行环境
- 支持同步、异步、响应式场景
- 确保上下文正确传递

这种设计真正实现了对 MCP Java SDK 的**非侵入式扩展**，同时为 Spring AI 提供了**丰富的增强功能**。
