# Spring AI MCP Plus

Spring AI MCP Plus 是一个非侵入式增强库，为 Spring AI 的 Model Context Protocol (MCP) 提供 FastMCP 风格的高级功能。

## ✨ 特性

- 🔧 **非侵入式设计** - 现有 Spring AI 工具无需修改，自动获得增强功能
- 📝 **丰富的上下文管理** - 会话存储、认证信息、请求元数据一应俱全
- 🔐 **安全增强** - 内置认证支持和上下文隔离
- 🧵 **线程安全** - 自动检测环境，支持 Servlet、Reactive、Async
- 🔌 **MCP集成** - 扩展 MCP Java SDK 的 McpServerSession
- 🚀 **Spring Boot 原生** - 零配置启用，遵循 Spring 设计原则

## 🚀 5分钟快速体验

### 1. 添加依赖

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-plus-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 写一个工具

```java
@Component
public class MyService {
    
    @Tool(name = "remember", description = "Remember user names")
    public String remember(@Parameter("name") String name) {
        // 获取会话，跨调用保持状态
        McpPlusSession session = McpPlusContextAccessor.getCurrentSession();
        
        List<String> names = session.getData("names", List.class)
            .orElse(new ArrayList<>());
        
        if (!names.contains(name)) {
            names.add(name);
            session.setData("names", names);
            return "Nice to meet you, " + name + "!";
        }
        return "Welcome back, " + name + "!";
    }
}
```

### 3. 就这样！

✅ 现有工具自动获得会话管理  
✅ 自动提取HTTP认证信息  
✅ 线程安全的上下文传播  
✅ 与MCP Server Session无缝集成  

**👉 [5分钟快速入门教程](QUICK_START.md)**

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
