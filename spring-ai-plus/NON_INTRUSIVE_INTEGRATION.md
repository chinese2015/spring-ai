# 非侵入式集成策略实现

## 🎯 设计原则

基于用户反馈，我们采用了完全非侵入式的集成策略：

1. **保持现有Spring AI功能不变** - 不重写TransportProvider
2. **线程安全优先** - 修复并发和内存泄漏问题  
3. **轻量级包装** - 只增强，不替换
4. **自动发现** - 透明的Session获取

## 🏗️ 架构概览

```
Spring AI MCP (原生)
├── StdioServerTransportProvider    ✅ 保持不变
├── WebMvcServerTransportProvider   ✅ 保持不变  
├── WebFluxServerTransportProvider  ✅ 保持不变
└── McpServerSession                ✅ 保持不变

MCP Plus (增强层)
├── McpSessionRegistrar            🆕 非侵入式Session包装
├── McpPlusToolAspect             🆕 AOP自动增强
├── ThreadSafeContextHolder       🆕 线程安全Context管理
└── EnhancedMcpServerSession      🆕 Session包装器
```

## 🔧 核心组件

### 1. ThreadSafeContextHolder - 解决线程安全问题

**问题**：原始的`AdaptiveContextHolder`存在：
- 同时使用多种ThreadLocal策略
- 动态环境检测的性能开销  
- 潜在的内存泄漏

**解决方案**：
```java
public class ThreadSafeContextHolder implements McpPlusContextHolder {
    // 缓存环境检测结果，避免重复检测
    private static final Environment DETECTED_ENV = EnvironmentDetector.detectEnvironment();
    
    // 统一的ThreadLocal策略
    private final ThreadLocal<McpPlusContext> contextHolder = new ThreadLocal<>();
    
    // 支持线程池传播（可选）
    private static final boolean TTL_AVAILABLE = isTransmittableThreadLocalAvailable();
    
    // 自动资源清理
    @PreDestroy
    public void cleanup() {
        clearContext();
    }
}
```

### 2. McpSessionRegistrar - 非侵入式Session管理

**策略**：不修改Spring AI的TransportProvider，而是包装已创建的Session：

```java
public class McpSessionRegistrar {
    private final Map<String, EnhancedMcpServerSession> sessionRegistry = new ConcurrentHashMap<>();
    
    // 包装现有Session，而不是替换
    public EnhancedMcpServerSession registerOrGet(Object mcpServerSession) {
        return sessionRegistry.computeIfAbsent(sessionId, id -> {
            EnhancedMcpServerSession enhanced = new EnhancedMcpServerSession(mcpServerSession);
            sessionManager.saveSession(enhanced.getApplicationSession());
            return enhanced;
        });
    }
}
```

### 3. McpPlusToolAspect - AOP自动发现

**策略**：通过AOP拦截工具调用，自动发现当前MCP Session：

```java
@Aspect
public class McpPlusToolAspect {
    
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object enhanceToolExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        // 多策略Session发现
        Object mcpServerSession = discoverCurrentMcpSession();
        
        if (mcpServerSession != null) {
            // 包装并设置增强上下文
            EnhancedMcpServerSession enhanced = sessionRegistrar.registerOrGet(mcpServerSession);
            McpPlusContext context = McpPlusContextBridge.fromMcpServerSession(mcpServerSession);
            
            return McpPlusContextAccessor.runWithContext(context, () -> 
                joinPoint.proceed()
            );
        }
        
        return joinPoint.proceed(); // 正常执行
    }
}
```

## 🔍 Session发现策略

### 多层次发现机制

1. **Spring AI内部上下文** - 查找ThreadLocal或Bean作用域中的Session
2. **Web请求属性** - 在Servlet环境中查找请求属性
3. **Reactive上下文** - 在WebFlux环境中查找Reactor Context
4. **调用栈分析** - 作为后备策略分析调用链

```java
private Object discoverCurrentMcpSession() {
    // Strategy 1: Spring AI internal context
    Object session = tryGetFromSpringAiContext();
    if (session != null) return session;
    
    // Strategy 2: Web request attributes  
    session = tryGetFromWebRequest();
    if (session != null) return session;
    
    // Strategy 3: Reactive context
    session = tryGetFromReactiveContext();
    if (session != null) return session;
    
    // Strategy 4: Call stack analysis (fallback)
    return tryGetFromCallStack();
}
```

## ⚙️ 配置选项

```yaml
spring:
  ai:
    mcp:
      plus:
        enabled: true
        aop:
          enabled: true                    # AOP自动增强
          session-discovery:
            spring-ai-context: true        # 从Spring AI内部查找
            web-request: true              # 从Web请求查找
            reactive-context: true         # 从Reactive上下文查找
            call-stack: false              # 调用栈分析（性能影响）
        context:
          holder-type: THREAD_SAFE         # 使用线程安全Holder
          cleanup-on-shutdown: true       # 自动清理
```

## 🚀 使用示例

### 自动增强（推荐）

```java
@Component
public class MyToolService {
    
    @Tool(name = "getUserData", description = "Get user data")
    public UserData getUserData(@Parameter("userId") String userId) {
        // 自动获取增强上下文 - 无需任何修改！
        McpPlusContext context = McpPlusContextAccessor.getCurrentContext();
        
        // 访问MCP Session
        Object mcpSession = context.getSession()
            .getUnderlyingMcpServerSession()
            .orElse(null);
            
        // 访问会话数据
        String authToken = context.getAuthToken().orElse("anonymous");
        
        return userService.getUserData(userId, authToken);
    }
}
```

### 手动增强（高级场景）

```java
@Service  
public class AdvancedMcpService {
    
    private final McpSessionRegistrar sessionRegistrar;
    
    public void processWithKnownSession(Object mcpServerSession) {
        // 手动注册Session
        EnhancedMcpServerSession enhanced = sessionRegistrar.registerOrGet(mcpServerSession);
        
        // 创建上下文
        McpPlusContext context = McpPlusContextBridge.fromMcpServerSession(mcpServerSession);
        
        // 执行业务逻辑
        McpPlusContextAccessor.runWithContext(context, () -> {
            toolService.executeTools();
        });
    }
}
```

## ✅ 优势总结

### 1. **完全非侵入**
- ✅ 不修改Spring AI现有代码
- ✅ 兼容所有TransportProvider
- ✅ 保持现有配置不变

### 2. **线程安全**
- ✅ 修复内存泄漏问题
- ✅ 统一ThreadLocal策略
- ✅ 自动资源清理

### 3. **自动发现**
- ✅ 透明的Session获取
- ✅ 多策略发现机制
- ✅ 性能优化缓存

### 4. **易于使用**
- ✅ 对现有代码透明
- ✅ 自动配置支持
- ✅ 灵活的配置选项

## 🔄 迁移指南

从原来的侵入式策略迁移到新策略：

### 之前（侵入式）
```java
// 需要自定义TransportProvider ❌
@Bean
@Primary 
public McpServerTransportProvider customProvider() {
    return new McpPlusAwareTransportProvider();
}
```

### 现在（非侵入式）
```java
// 什么都不需要改！✅
// 自动配置会处理一切
```

只需要确保依赖中包含：
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-plus-starter</artifactId>
</dependency>
```

这种策略既保持了Spring AI的完整功能，又提供了我们需要的Plus增强能力！🎉
