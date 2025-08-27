# 新的非侵入式集成策略

## 问题分析

基于用户反馈，当前的集成策略存在以下问题：

1. **过度侵入**：试图重写TransportProvider，可能影响Spring AI现有功能
2. **线程安全问题**：AdaptiveContextHolder存在并发和内存泄漏风险
3. **复杂性过高**：重新实现已有的传输层功能

## 新策略：轻量级包装

### 1. 保持现有TransportProvider不变

Spring AI已经提供了完整的传输层：
- StdioServerTransportProvider
- WebMvcServerTransportProvider  
- WebFluxServerTransportProvider
- 丰富的配置和自动装配

**我们的原则**：不重写，只增强！

### 2. Session获取策略

#### 方案A：AOP拦截（推荐）
```java
@Aspect
@Component
public class McpPlusAspect {
    
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object enhanceToolExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        // 通过Spring的执行上下文查找McpServerSession
        McpServerSession mcpSession = findCurrentMcpSession();
        
        if (mcpSession != null) {
            McpPlusContext context = createEnhancedContext(mcpSession);
            return McpPlusContextAccessor.runWithContext(context, () -> 
                joinPoint.proceed()
            );
        }
        
        return joinPoint.proceed();
    }
    
    private McpServerSession findCurrentMcpSession() {
        // 通过ThreadLocal、RequestContext等查找
        // 或者通过Spring的工具执行上下文
        return getCurrentMcpSession();
    }
}
```

#### 方案B：事件监听
```java
@EventListener
public void onMcpToolExecution(ToolExecutionEvent event) {
    if (event.hasSessionContext()) {
        McpServerSession mcpSession = event.getMcpSession();
        // 包装和增强
        enhanceWithPlusContext(mcpSession);
    }
}
```

#### 方案C：ToolCallback装饰器
```java
@Component
public class McpPlusToolCallbackDecorator implements ToolCallback {
    
    private final ToolCallback delegate;
    
    @Override
    public String call(String toolName, String arguments) {
        McpServerSession mcpSession = extractFromCurrentExecution();
        
        if (mcpSession != null) {
            return withEnhancedContext(mcpSession, () -> 
                delegate.call(toolName, arguments)
            );
        }
        
        return delegate.call(toolName, arguments);
    }
}
```

### 3. 线程安全的ContextHolder重构

#### 问题分析
当前的AdaptiveContextHolder存在：
- 同时使用多种ThreadLocal策略
- 动态环境检测的性能开销
- 潜在的内存泄漏

#### 解决方案
```java
public class ThreadSafeContextHolder implements McpPlusContextHolder {
    
    // 使用TransmittableThreadLocal支持线程池传播
    private static final TransmittableThreadLocal<McpPlusContext> contextHolder = 
        new TransmittableThreadLocal<>();
    
    // 环境检测结果缓存，避免重复检测
    private static final Environment DETECTED_ENV = EnvironmentDetector.detectEnvironment();
    
    @Override
    public void setContext(McpPlusContext context) {
        switch (DETECTED_ENV) {
            case SERVLET:
                setServletContext(context);
                break;
            case REACTIVE:
                setReactiveContext(context);
                break;
            case ASYNC:
                contextHolder.set(context);
                break;
            default:
                contextHolder.set(context);
        }
    }
    
    @Override
    public McpPlusContext getContext() {
        // 统一的获取逻辑，避免重复环境检测
        return switch (DETECTED_ENV) {
            case SERVLET -> getServletContext();
            case REACTIVE -> getReactiveContext(); 
            case ASYNC -> contextHolder.get();
            default -> contextHolder.get();
        } ?: McpPlusContext.empty();
    }
    
    @Override
    public void clearContext() {
        contextHolder.remove();
        clearEnvironmentSpecificContext();
    }
    
    @PreDestroy
    public void cleanup() {
        // 确保资源清理
        contextHolder.remove();
    }
}
```

### 4. 实现计划

#### 阶段1：修复线程安全问题
1. 重构AdaptiveContextHolder
2. 添加TransmittableThreadLocal支持
3. 优化环境检测逻辑
4. 添加资源清理

#### 阶段2：实现非侵入式Session包装
1. AOP拦截器实现
2. Session注册表
3. 自动发现机制
4. Spring Boot自动配置

#### 阶段3：集成测试
1. 多种传输方式测试
2. 并发场景测试
3. 内存泄漏测试
4. 性能影响评估

## 优势

1. **完全非侵入**：不修改Spring AI现有功能
2. **兼容性好**：支持所有现有TransportProvider
3. **线程安全**：正确的并发控制
4. **性能优化**：避免重复环境检测
5. **易于使用**：对用户透明的增强

这种策略既保持了Spring AI的完整功能，又提供了我们需要的Plus增强能力！
