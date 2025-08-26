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

## 📚 文档和示例

| 文档 | 描述 |
|------|------|
| **[快速入门](QUICK_START.md)** | 5分钟上手体验 |
| **[使用指南](USAGE_GUIDE.md)** | 完整API和使用场景 |
| **[示例项目](examples/)** | 可运行的完整示例 |
| **[架构设计](detailed_design.md)** | 技术架构和设计理念 |

## 🎯 使用场景

### 基础增强 - 无需修改现有代码
```java
// 现有Spring AI工具自动获得增强功能
@Tool(name = "existingTool")
public String existingTool(String input) {
    return "result"; // 自动支持会话、认证等
}
```

### 会话管理 - 跨调用保持状态
```java
@Tool(name = "chatTool")
public String chat(String message) {
    McpPlusSession session = McpPlusContextAccessor.getCurrentSession();
    List<String> history = session.getData("history", List.class).orElse(new ArrayList<>());
    history.add(message);
    session.setData("history", history);
    return "Received: " + message + " (history: " + history.size() + ")";
}
```

### 认证集成 - 安全的工具调用
```java
@Tool(name = "secureTool")
public String secure(String data) {
    String token = McpPlusContextAccessor.getAuthToken()
        .orElseThrow(() -> new SecurityException("Authentication required"));
    return processSecurely(data, token);
}
```

### MCP集成 - 扩展协议会话
```java
public void handleMcpRequest(Object mcpServerSession, String input) {
    McpPlusContext context = McpPlusContextBridge.fromMcpServerSession(mcpServerSession);
    McpPlusContextAccessor.runWithContext(context, () -> {
        return toolService.process(input); // 工具可访问协议会话信息
    });
}
```

## ⚙️ 配置选项

```yaml
spring:
  ai:
    mcp:
      plus:
        enabled: true
        context:
          session:
            max-inactive-interval: PT30M
          thread-safety:
            strategy: adaptive  # 自动检测环境
```

## 🏗️ 架构特点

### 三层清晰架构
```
┌─ MCP Plus 应用层 ─────────────────────────┐
│  McpPlusContext + McpPlusSession          │ ← 业务增强功能
├─ Spring AI 工具执行层 ────────────────────┤  
│  ToolContext + ToolCallback               │ ← 工具执行基础
├─ MCP Java SDK 协议层 ─────────────────────┤
│  McpServerSession + JSON-RPC             │ ← 协议通信管理  
└─────────────────────────────────────────────┘
```

### 核心设计原则
- 🔄 **非侵入式** - 装饰器模式，不修改现有代码
- 🔗 **向后兼容** - 现有Spring AI工具零修改即可使用
- 🎯 **职责分离** - 协议层、工具层、应用层各司其职
- 🌐 **环境感知** - 自动适配Servlet/Reactive/Async环境

## 🔗 集成方式

### 与现有Spring AI项目集成
1. **添加依赖** → 自动获得增强功能
2. **无需修改现有工具** → 继续正常工作
3. **按需使用新功能** → 渐进式采用

### 与MCP Java SDK集成
```java
// 扩展MCP Server Session
EnhancedMcpServerSession enhanced = new EnhancedMcpServerSession(mcpServerSession);

// 桥接不同层次的上下文
McpPlusContext context = McpPlusContextBridge.fromBothContexts(toolContext, mcpServerSession);
```

## 🤝 贡献

欢迎提交Issues和Pull Requests！

- 📋 **Issues** - 报告bug或请求新功能
- 🔀 **Pull Requests** - 代码贡献
- 📖 **文档** - 改进文档和示例

## 📄 许可证

Apache License 2.0

---

## 🚀 开始使用

**[👉 5分钟快速入门](QUICK_START.md)** | **[📚 完整文档](USAGE_GUIDE.md)** | **[🔍 示例代码](examples/)**
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

