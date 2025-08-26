# McpServerSession 获取和集成指南

## 问题背景

在使用Spring AI MCP Plus库时，一个关键问题是：**如何获取`McpServerSession`并将其传递给我们的增强上下文？**

这涉及到MCP Java SDK的内部机制以及Spring AI MCP的集成方式。

## McpServerSession的生命周期

### 1. MCP Java SDK中的Session创建流程

```
客户端连接 → McpServerTransportProvider → McpServerSession.Factory → McpServerSession实例
```

在MCP Java SDK中，`McpServerSession`的创建过程如下：

```java
// 1. 设置传输提供者和会话工厂
McpServer server = McpServer.sync(transportProvider)
    .tools(toolSpecifications)
    .build();

// 2. 当客户端连接时，传输提供者创建session
public class CustomTransportProvider implements McpServerTransportProvider {
    private McpServerSession.Factory sessionFactory;
    
    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    // 当新客户端连接时调用
    protected void onClientConnected(McpServerTransport transport) {
        McpServerSession session = sessionFactory.create(transport);
        // 这里可以获取到session实例
        handleNewSession(session);
    }
}
```

### 2. Spring AI MCP的集成方式

Spring AI通过自动配置创建MCP服务器：

```java
// Spring AI MCP自动配置中
@Bean
public McpServerTransportProvider stdioServerTransport() {
    return new StdioServerTransportProvider();
}

// 创建同步或异步服务器
@Bean
public McpSyncServer mcpSyncServer(
    McpServerTransportProvider transportProvider,
    List<McpServerFeatures.SyncToolSpecification> tools) {
    
    return McpServer.sync(transportProvider)
        .tools(tools)
        .build();
}
```

## 实际集成策略

### 方案1：自定义TransportProvider（推荐）

这是最直接的方式，通过自定义传输提供者来拦截session创建：

```java
@Component
public class McpPlusAwareTransportProvider implements McpServerTransportProvider {
    
    private McpServerSession.Factory sessionFactory;
    private final McpPlusSessionManager sessionManager;
    
    public McpPlusAwareTransportProvider(McpPlusSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    @Override
    public void start() {
        // 启动传输层，监听客户端连接
        startListening();
    }
    
    protected void onClientConnected(McpServerTransport transport) {
        // 创建MCP协议层session
        McpServerSession mcpSession = sessionFactory.create(transport);
        
        // 创建我们的增强session
        EnhancedMcpServerSession enhancedSession = 
            new EnhancedMcpServerSession(mcpSession);
        
        // 注册到session管理器
        sessionManager.registerSession(enhancedSession);
        
        // 设置消息处理器
        mcpSession.setRequestHandler(request -> {
            // 在处理请求时，设置Plus上下文
            McpPlusContext context = McpPlusContextBridge
                .fromMcpServerSession(mcpSession);
            
            return McpPlusContextAccessor.runWithContext(context, () -> {
                return handleMcpRequest(request);
            });
        });
    }
    
    private McpSchema.JSONRPCResponse handleMcpRequest(McpSchema.JSONRPCRequest request) {
        // 在这个作用域内，所有工具都可以访问增强上下文
        // Spring AI的工具会被自动调用
        return processRequest(request);
    }
}
```

### 方案2：通过Spring AI MCP Hook集成

利用Spring AI MCP的扩展点：

```java
@Configuration
public class McpPlusIntegrationConfig {
    
    @Bean
    @Primary
    public McpServerTransportProvider enhancedTransportProvider(
            @Qualifier("stdioServerTransport") McpServerTransportProvider originalProvider,
            McpPlusSessionManager sessionManager) {
        
        return new McpPlusTransportProviderWrapper(originalProvider, sessionManager);
    }
}

public class McpPlusTransportProviderWrapper implements McpServerTransportProvider {
    
    private final McpServerTransportProvider delegate;
    private final McpPlusSessionManager sessionManager;
    private McpServerSession.Factory sessionFactory;
    
    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
        
        // 包装原始的session factory
        McpServerSession.Factory enhancedFactory = transport -> {
            McpServerSession originalSession = sessionFactory.create(transport);
            
            // 创建增强session
            EnhancedMcpServerSession enhanced = 
                new EnhancedMcpServerSession(originalSession);
            
            // 注册到管理器
            sessionManager.registerSession(enhanced);
            
            return createEnhancedSessionProxy(originalSession, enhanced);
        };
        
        delegate.setSessionFactory(enhancedFactory);
    }
    
    private McpServerSession createEnhancedSessionProxy(
            McpServerSession original, 
            EnhancedMcpServerSession enhanced) {
        
        return new McpServerSessionProxy(original) {
            @Override
            public Mono<McpSchema.JSONRPCResponse> handle(McpSchema.JSONRPCRequest request) {
                // 设置Plus上下文
                McpPlusContext context = McpPlusContextBridge
                    .fromMcpServerSession(original);
                
                return McpPlusContextAccessor.runWithContextMono(context, () -> {
                    return super.handle(request);
                });
            }
        };
    }
}
```

### 方案3：HTTP Transport集成（适用于Web环境）

对于使用HTTP传输的场景：

```java
@RestController
@RequestMapping("/mcp")
public class McpPlusController {
    
    private final McpAsyncServer mcpServer;
    private final McpPlusSessionManager sessionManager;
    
    @PostMapping("/message")
    public Mono<ResponseEntity<String>> handleMessage(
            @RequestBody String message,
            HttpServletRequest request) {
        
        // 从HTTP请求创建Plus上下文
        McpPlusContext httpContext = createContextFromHttpRequest(request);
        
        // 获取或创建MCP session
        String sessionId = extractSessionId(request);
        McpServerSession mcpSession = getOrCreateMcpSession(sessionId);
        
        // 创建综合上下文
        McpPlusContext fullContext = McpPlusContextBridge
            .fromBothContexts(null, mcpSession);
        
        return McpPlusContextAccessor.runWithContextMono(fullContext, () -> {
            // 处理MCP消息
            return mcpServer.handle(parseMessage(message))
                .map(response -> ResponseEntity.ok(response.toString()));
        });
    }
    
    private McpPlusContext createContextFromHttpRequest(HttpServletRequest request) {
        // 提取认证、客户端信息等
        String authToken = request.getHeader("Authorization");
        String clientId = request.getHeader("X-Client-ID");
        
        DefaultMcpPlusContext context = new DefaultMcpPlusContext();
        if (authToken != null) {
            context.setAttribute("authToken", authToken);
        }
        if (clientId != null) {
            context.setAttribute("clientId", clientId);
        }
        
        return context;
    }
}
```

## 实际使用示例

### 1. 配置增强传输提供者

```java
@Configuration
@EnableMcpPlus
public class McpPlusConfiguration {
    
    // 替换默认的传输提供者
    @Bean
    @Primary
    public McpServerTransportProvider mcpPlusTransportProvider(
            McpPlusSessionManager sessionManager) {
        return new McpPlusAwareStdioTransportProvider(sessionManager);
    }
}
```

### 2. 在工具中使用增强上下文

```java
@Component
public class EnhancedToolService {
    
    @Tool(name = "getUserData", description = "Get user data with session")
    public UserData getUserData(@Parameter("userId") String userId) {
        // 自动获取增强上下文
        McpPlusContext context = McpPlusContextAccessor.getCurrentContext();
        
        // 访问MCP协议层信息
        Object mcpSession = context.getSession()
            .getUnderlyingMcpServerSession()
            .orElse(null);
        
        if (mcpSession != null) {
            // 可以访问协议层信息，如客户端capabilities等
            logger.info("Processing request from MCP client");
        }
        
        // 访问会话数据
        McpPlusSession session = context.getSession();
        Optional<UserPreferences> prefs = session.getData("userPrefs", UserPreferences.class);
        
        // 访问认证信息
        String authToken = context.getAuthToken()
            .orElseThrow(() -> new SecurityException("Authentication required"));
        
        return userService.getUserData(userId, authToken, prefs.orElse(null));
    }
}
```

### 3. 手动管理session（高级用法）

```java
@Service
public class McpPlusSessionService {
    
    private final Map<String, EnhancedMcpServerSession> sessions = new ConcurrentHashMap<>();
    
    public void handleNewMcpClient(McpServerSession mcpSession) {
        // 创建增强session
        EnhancedMcpServerSession enhanced = new EnhancedMcpServerSession(mcpSession);
        
        // 注册session
        String sessionId = generateSessionId();
        sessions.put(sessionId, enhanced);
        
        // 设置消息处理
        mcpSession.setRequestHandler(request -> {
            return handleWithPlusContext(enhanced, request);
        });
    }
    
    private Mono<McpSchema.JSONRPCResponse> handleWithPlusContext(
            EnhancedMcpServerSession enhanced, 
            McpSchema.JSONRPCRequest request) {
        
        McpPlusContext context = McpPlusContextBridge
            .fromMcpServerSession(enhanced.getMcpServerSession());
        
        return McpPlusContextAccessor.runWithContextMono(context, () -> {
            // 工具调用会自动获取到这个上下文
            return processToolCall(request);
        });
    }
}
```

## 最佳实践

### 1. 自动集成（推荐）

最简单的方式是通过自定义传输提供者实现自动集成：

```java
// 只需要配置一个Bean
@Bean
@Primary
public McpServerTransportProvider enhancedTransport(McpPlusSessionManager manager) {
    return new McpPlusStdioTransportProvider(manager);
}
```

### 2. 按需集成

如果只在特定场景下需要MCP session集成：

```java
// 在需要时手动创建上下文
public void processMcpRequest(McpServerSession mcpSession, String request) {
    McpPlusContext context = McpPlusContextBridge.fromMcpServerSession(mcpSession);
    
    McpPlusContextAccessor.runWithContext(context, () -> {
        toolService.processRequest(request);
    });
}
```

### 3. 混合模式

支持同时使用HTTP和MCP协议：

```java
@RestController
public class UnifiedController {
    
    // HTTP端点
    @PostMapping("/api/tools")
    public String httpTool(@RequestBody String input, HttpServletRequest request) {
        McpPlusContext httpContext = createFromHttp(request);
        return executeWithContext(httpContext, input);
    }
    
    // MCP消息处理
    public String mcpTool(McpServerSession mcpSession, String input) {
        McpPlusContext mcpContext = McpPlusContextBridge.fromMcpServerSession(mcpSession);
        return executeWithContext(mcpContext, input);
    }
    
    private String executeWithContext(McpPlusContext context, String input) {
        return McpPlusContextAccessor.runWithContext(context, () -> {
            return toolService.execute(input);
        });
    }
}
```

通过这些方式，您可以根据具体需求选择合适的`McpServerSession`集成策略，实现协议层和应用层的无缝桥接！
