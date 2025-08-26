# Spring AI MCP Plus 示例

这个目录包含了Spring AI MCP Plus的各种使用示例。

## 示例列表

### 1. [Simple Usage](simple-usage/) - 基础使用示例

展示如何在Spring Boot应用中使用MCP Plus的基本功能：

- **基础工具增强**：无需修改现有代码即可获得增强功能
- **会话管理**：在工具调用间保持状态
- **认证集成**：从HTTP头提取认证信息
- **上下文传播**：在不同组件间传递上下文

**运行方式：**
```bash
cd examples/simple-usage
mvn spring-boot:run
```

**测试API：**
```bash
# 基础计算
curl -X POST http://localhost:8080/api/calculate \
  -H "Content-Type: application/json" \
  -d '{"expression": "10 + 5"}'

# 认证计算
curl -X POST http://localhost:8080/api/secure-calculate \
  -H "Content-Type: application/json" \
  -H "Authorization: valid-token" \
  -H "X-Client-ID: test-client" \
  -d '{"expression": "20 * 3"}'

# 会话管理
curl -X POST "http://localhost:8080/api/session/set?key=username&value=john"
curl -X POST "http://localhost:8080/api/session/get?key=username"
curl -X POST "http://localhost:8080/api/session/info"
```

### 2. [MCP Integration](mcp-integration/) - McpServerSession集成示例

**解答关键问题：如何获取McpServerSession并传递给MCP Plus？**

展示完整的MCP Java SDK集成方案：

- **McpServerSession获取**：通过自定义TransportProvider拦截session创建
- **协议层桥接**：使用EnhancedMcpServerSession桥接协议层和应用层
- **上下文传播**：McpPlusContextBridge实现多层上下文集成
- **实际使用场景**：在工具中同时访问协议信息和业务数据

**核心特性：**
- ✅ 自动拦截MCP客户端连接事件
- ✅ 创建增强session包装协议session
- ✅ 在工具调用中访问MCP协议层信息
- ✅ 完整的上下文生命周期管理

**运行示例：**
```bash
cd examples/mcp-integration
mvn spring-boot:run
```

## 即将添加的示例

### 3. Middleware Example - 中间件示例  
演示如何使用中间件进行认证、日志、限流等

### 4. Reactive Usage - 响应式使用
在WebFlux环境中使用MCP Plus

### 5. Advanced Session Management - 高级会话管理
自定义会话存储（Redis、数据库等）

## 如何运行示例

每个示例都是独立的Spring Boot应用，可以单独运行：

1. 确保已安装JDK 17+和Maven 3.6+
2. 进入示例目录
3. 运行 `mvn spring-boot:run`
4. 查看各示例的README了解具体API

## 示例特点

- **渐进式复杂度**：从简单到复杂，逐步展示功能
- **实际场景**：基于真实使用场景设计
- **完整代码**：可以直接运行的完整示例
- **详细注释**：代码中包含详细的说明注释
