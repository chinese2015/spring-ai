# Spring AI MCP Plus

Spring AI MCP Plus 是一个非侵入式增强库，为 Spring AI MCP 提供类似 FastMCP 的高级特性。

## 特性

- ✅ **上下文增强系统** - 提供丰富的上下文访问机制，支持会话管理、请求元数据访问和认证信息
- 🔄 **自适应上下文传播** - 根据运行环境自动选择最适合的上下文传播策略 
- 🧵 **线程安全** - 完全的线程安全设计，支持多线程和异步环境
- ⚙️ **零配置启用** - 添加依赖即可自动工作，无需额外配置
- 📦 **Spring Boot 集成** - 原生支持 Spring Boot 自动配置

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-plus-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 使用上下文功能

```java
import org.springframework.ai.mcp.plus.core.context.*;

// 获取当前上下文
McpPlusContext context = McpPlusContextAccessor.getCurrentContext();

// 访问会话数据
McpPlusSession session = context.getSession();
session.setData("userPreferences", userPrefs);
Optional<UserPreferences> prefs = session.getData("userPreferences", UserPreferences.class);

// 访问请求元数据
RequestMetadata metadata = context.getRequestMetadata();
String transportType = metadata.getTransportType();

// 访问认证信息
Optional<String> authToken = context.getAuthToken();
Optional<String> clientId = context.getClientId();

// 设置自定义属性
context.setAttribute("customData", "value");
Optional<String> data = context.getAttribute("customData", String.class);
```

### 3. 配置选项

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
            strategy: adaptive  # adaptive, thread_local, inheritable
            inheritable-thread-local: true
```

## 架构设计

### 核心组件

1. **McpPlusContext** - 增强的上下文接口，扩展了 Spring AI 的 ToolContext
2. **McpPlusSession** - 会话管理，支持session-scoped数据存储
3. **AdaptiveContextHolder** - 自适应的上下文持有者，根据环境选择传播策略
4. **EnvironmentDetector** - 环境检测器，自动识别 Servlet/Reactive/Async 环境

### 设计原则

- **非侵入式** - 不修改 Spring AI 源码，通过装饰器模式扩展功能
- **向后兼容** - 完全兼容现有的 Spring AI MCP API
- **环境感知** - 自动适配不同的运行环境（Servlet、Reactive、Async）

## API 文档

### McpPlusContext

```java
public interface McpPlusContext {
    // 兼容 Spring AI
    ToolContext getToolContext();
    
    // 会话管理
    McpPlusSession getSession();
    Optional<String> getSessionId();
    
    // 请求元数据
    RequestMetadata getRequestMetadata();
    Map<String, String> getHeaders();
    Optional<String> getHeader(String name);
    
    // 认证信息
    Optional<String> getAuthToken();
    Optional<String> getClientId();
    
    // 属性管理
    <T> Optional<T> getAttribute(String name, Class<T> type);
    void setAttribute(String name, Object value);
    void removeAttribute(String name);
    Map<String, Object> getAttributes();
}
```

### McpPlusSession

```java
public interface McpPlusSession {
    // 会话标识
    String getId();
    Instant getCreatedAt();
    Instant getLastAccessedAt();
    
    // 生命周期管理
    Duration getMaxInactiveInterval();
    boolean isExpired();
    void invalidate();
    void touch();
    
    // 数据存储
    <T> Optional<T> getData(String key, Class<T> type);
    void setData(String key, Object value);
    void removeData(String key);
    Map<String, Object> getAllData();
}
```

### 便捷访问器

```java
// 静态访问方法
McpPlusContext context = McpPlusContextAccessor.getCurrentContext();
McpPlusSession session = McpPlusContextAccessor.getCurrentSession();
Optional<String> token = McpPlusContextAccessor.getAuthToken();

// 会话数据快捷访问
McpPlusContextAccessor.setSessionData("key", value);
Optional<String> data = McpPlusContextAccessor.getSessionData("key", String.class);

// 上下文执行
McpPlusContextAccessor.runWithContext(context, () -> {
    // 在指定上下文中执行代码
});
```

## 测试

项目包含完整的单元测试，验证核心功能：

```bash
mvn test
```

测试覆盖：
- ✅ 上下文创建和管理
- ✅ 会话生命周期和数据存储
- ✅ 线程安全性验证
- ✅ 环境检测功能
- ✅ 异常处理

## 构建

```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 打包
mvn clean package
```

## 模块说明

- `spring-ai-mcp-plus-core` - 核心功能实现
- `spring-ai-mcp-plus-autoconfigure` - Spring Boot 自动配置
- `spring-ai-mcp-plus-starter` - Spring Boot Starter

## 未来计划

- 🔄 中间件框架 - 支持认证、日志、限流等横切关注点
- 🏗️ 服务器组合 - 支持多个 MCP 服务的模块化组合
- 🌉 代理服务器 - 协议转换和请求路由功能
- 📋 OpenAPI 集成 - 从 OpenAPI 规范自动生成 MCP 工具

## 许可证

Apache License 2.0
