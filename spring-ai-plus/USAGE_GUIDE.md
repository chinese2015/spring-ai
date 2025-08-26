# Spring AI MCP Plus 使用指南

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-plus-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 启用 MCP Plus

```java
@SpringBootApplication
@EnableMcpPlus // 可选，默认会自动启用
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 基础配置

```yaml
spring:
  ai:
    mcp:
      plus:
        enabled: true
        context:
          session:
            max-inactive-interval: PT30M
```

## 使用场景

### 场景1：增强现有Spring AI工具（最简单）

如果你已经有Spring AI工具，无需修改任何代码，自动获得增强功能：

```java
@Component
public class UserService {
    
    // 现有工具，无需修改
    @Tool(name = "getUser", description = "Get user by ID")
    public User getUser(@Parameter("userId") String userId) {
        return userRepository.findById(userId);
    }
    
    // 新增强工具，使用MCP Plus功能
    @Tool(name = "getUserWithSession", description = "Get user with session context")
    public User getUserWithSession(@Parameter("userId") String userId) {
        // 获取增强上下文
        McpPlusContext context = McpPlusContextAccessor.getCurrentContext();
        
        // 访问会话数据
        McpPlusSession session = context.getSession();
        Optional<UserPreferences> prefs = session.getData("preferences", UserPreferences.class);
        
        // 访问认证信息
        Optional<String> authToken = context.getAuthToken();
        
        // 访问请求元数据
        RequestMetadata metadata = context.getRequestMetadata();
        String clientInfo = metadata.getClientUserAgent().orElse("Unknown");
        
        return userRepository.findByIdWithContext(userId, prefs.orElse(null), authToken.orElse(null));
    }
}
```

### 场景2：会话管理

```java
@RestController
public class ChatController {
    
    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody ChatRequest request) {
        
        // 设置会话上下文
        McpPlusContext context = McpPlusContext.empty();
        McpPlusSession session = context.getSession();
        
        // 存储会话数据
        session.setData("userId", request.getUserId());
        session.setData("chatHistory", new ArrayList<String>());
        
        // 执行工具调用
        return McpPlusContextAccessor.runWithContext(context, () -> {
            // 在这个作用域内，所有工具都可以访问会话数据
            String result = toolService.executeChat(request.getMessage());
            return ResponseEntity.ok(result);
        });
    }
}
```

### 场景3：认证和授权

```java
@Component
public class SecureService {
    
    @Tool(name = "secureOperation", description = "Secure operation")
    public OperationResult secureOperation(@Parameter("data") String data) {
        McpPlusContext context = McpPlusContextAccessor.getCurrentContext();
        
        // 检查认证
        String authToken = context.getAuthToken()
            .orElseThrow(() -> new SecurityException("Authentication required"));
            
        // 验证token
        if (!authService.validateToken(authToken)) {
            throw new SecurityException("Invalid token");
        }
        
        // 获取用户信息
        UserInfo user = authService.getUserInfo(authToken);
        
        // 存储到会话以便后续使用
        context.getSession().setData("currentUser", user);
        
        return performOperation(data, user);
    }
}
```

### 场景4：与MCP Server Session集成

```java
@Component
public class McpServerIntegration {
    
    private final McpServer mcpServer;
    
    public void handleMcpRequest(Object mcpServerSession, String toolName, String input) {
        
        // 从MCP Server Session创建增强上下文
        McpPlusContext context = McpPlusContextBridge.fromMcpServerSession(mcpServerSession);
        
        // 添加协议级信息
        if (context.getSession() instanceof DefaultMcpPlusSession defaultSession) {
            // 可以访问底层的MCP server session
            Object underlyingSession = defaultSession.getUnderlyingMcpServerSession().orElse(null);
            // 进行协议级操作...
        }
        
        // 执行工具
        McpPlusContextAccessor.runWithContext(context, () -> {
            ToolCallback tool = toolRegistry.findTool(toolName);
            return tool.call(input);
        });
    }
}
```

### 场景5：HTTP请求集成

```java
@RestController
public class ToolController {
    
    @PostMapping("/tools/{toolName}")
    public ResponseEntity<String> executeTool(
            @PathVariable String toolName,
            @RequestBody String input,
            HttpServletRequest request) {
        
        // 从HTTP请求创建上下文
        McpPlusContext context = createContextFromHttpRequest(request);
        
        return McpPlusContextAccessor.runWithContext(context, () -> {
            ToolCallback tool = toolRegistry.findTool(toolName);
            String result = tool.call(input);
            return ResponseEntity.ok(result);
        });
    }
    
    private McpPlusContext createContextFromHttpRequest(HttpServletRequest request) {
        // 提取认证信息
        String authToken = request.getHeader("Authorization");
        String clientId = request.getHeader("X-Client-ID");
        
        // 创建请求元数据
        RequestMetadata metadata = new DefaultRequestMetadata(
            "HTTP",
            Instant.now(),
            UUID.randomUUID().toString(),
            request.getRemoteAddr(),
            request.getHeader("User-Agent"),
            clientId
        );
        
        // 提取所有header
        Map<String, String> headers = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(name -> {
            headers.put(name, request.getHeader(name));
        });
        
        // 创建属性
        Map<String, Object> attributes = new HashMap<>();
        if (authToken != null) {
            attributes.put("authToken", authToken);
        }
        if (clientId != null) {
            attributes.put("clientId", clientId);
        }
        
        return new DefaultMcpPlusContext(null, new DefaultMcpPlusSession(), metadata, headers, attributes);
    }
}
```

## 高级功能

### 1. 自定义会话管理器

```java
@Configuration
public class CustomSessionConfig {
    
    @Bean
    @Primary
    public McpPlusSessionManager customSessionManager() {
        return new RedisSessionManager(); // 自定义实现
    }
}

public class RedisSessionManager implements McpPlusSessionManager {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public McpPlusSession createSession() {
        String sessionId = UUID.randomUUID().toString();
        McpPlusSession session = new DefaultMcpPlusSession(sessionId, Duration.ofMinutes(30), null);
        saveSession(session);
        return session;
    }
    
    @Override
    public Optional<McpPlusSession> getSession(String sessionId) {
        // 从Redis获取
        Object data = redisTemplate.opsForValue().get("session:" + sessionId);
        return data != null ? Optional.of((McpPlusSession) data) : Optional.empty();
    }
    
    // 其他方法实现...
}
```

### 2. 自定义上下文持有者

```java
@Configuration
public class CustomContextConfig {
    
    @Bean
    @Primary
    public McpPlusContextHolder customContextHolder() {
        // 使用自定义的上下文传播策略
        return new ReactiveContextHolder(); // 针对WebFlux应用
    }
}
```

### 3. 环境检测配置

```yaml
spring:
  ai:
    mcp:
      plus:
        context:
          thread-safety:
            strategy: inheritable  # thread_local, inheritable, adaptive
            force-environment: reactive  # servlet, reactive, async
```

## API参考

### McpPlusContext

```java
// 获取当前上下文
McpPlusContext context = McpPlusContextAccessor.getCurrentContext();

// 会话操作
McpPlusSession session = context.getSession();
session.setData("key", "value");
Optional<String> value = session.getData("key", String.class);

// 认证信息
Optional<String> token = context.getAuthToken();
Optional<String> clientId = context.getClientId();

// 请求元数据
RequestMetadata metadata = context.getRequestMetadata();
String transport = metadata.getTransportType();
Instant timestamp = metadata.getTimestamp();

// 自定义属性
context.setAttribute("customKey", customValue);
Optional<CustomType> custom = context.getAttribute("customKey", CustomType.class);

// Spring AI兼容性
ToolContext toolContext = context.getToolContext(); // 可能为null
```

### McpPlusSession

```java
McpPlusSession session = context.getSession();

// 基本信息
String id = session.getId();
Instant created = session.getCreatedAt();
Instant lastAccess = session.getLastAccessedAt();
boolean expired = session.isExpired();

// 数据操作
session.setData("user", userObject);
Optional<User> user = session.getData("user", User.class);
session.removeData("user");
Map<String, Object> allData = session.getAllData();

// 生命周期
session.touch(); // 更新最后访问时间
session.invalidate(); // 失效会话

// MCP集成
Optional<Object> mcpSession = session.getUnderlyingMcpServerSession();
```

### 上下文传播

```java
// 手动设置上下文
McpPlusContextAccessor.runWithContext(customContext, () -> {
    // 在这里执行的代码都可以访问customContext
    doSomething();
});

// 检查上下文
boolean hasContext = McpPlusContextAccessor.hasContext();

// 清理上下文
McpPlusContextAccessor.clearContext();

// 获取特定数据的便捷方法
Optional<String> authToken = McpPlusContextAccessor.getAuthToken();
Optional<String> sessionData = McpPlusContextAccessor.getSessionData("key", String.class);
McpPlusContextAccessor.setSessionData("key", "value");
```

## 最佳实践

### 1. 会话数据管理

```java
// ✅ 好的做法
session.setData("userPreferences", userPrefs);
Optional<UserPreferences> prefs = session.getData("userPreferences", UserPreferences.class);

// ❌ 避免存储大对象
// session.setData("largeData", hugeBinaryData); // 会影响性能
```

### 2. 上下文传播

```java
// ✅ 使用runWithContext确保正确清理
McpPlusContextAccessor.runWithContext(context, () -> {
    // 业务逻辑
});

// ❌ 手动设置后忘记清理
// McpPlusContextAccessor.setContext(context);
// doSomething(); // 如果出现异常，上下文可能不会被清理
```

### 3. 异常处理

```java
// ✅ 在工具中正确处理认证异常
@Tool(name = "secureOperation")
public Result secureOperation(String input) {
    try {
        String token = McpPlusContextAccessor.getAuthToken()
            .orElseThrow(() -> new SecurityException("Authentication required"));
        // 业务逻辑
    } catch (SecurityException e) {
        // 记录日志并返回适当的错误
        log.warn("Authentication failed for tool call", e);
        throw e;
    }
}
```

这样，Spring AI MCP Plus就提供了从简单的上下文增强到复杂的会话管理的完整解决方案！
