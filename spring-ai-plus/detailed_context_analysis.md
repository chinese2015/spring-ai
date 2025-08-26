# MCP Plus Context 架构分析与增强策略

## 当前架构的层次关系

### 1. MCP Java SDK 层 (协议层)
```
McpServerSession
├── JSON-RPC 消息处理
├── Transport 管理 (stdio, http, websocket)
├── 协议状态管理
├── 客户端连接管理
└── 协议版本协商
```

### 2. Spring AI 层 (工具执行层)
```
ToolContext
├── 简单的 Map<String, Object> 存储
├── Tool call history 管理
├── 不可变的上下文传递
└── 工具回调中的可选参数
```

### 3. MCP Plus 层 (应用增强层)
```
McpPlusContext
├── 包装 Spring AI ToolContext (向下兼容)
├── 扩展 会话管理 (McpPlusSession)
├── 增强 请求元数据 (RequestMetadata)
├── 支持 认证信息管理
├── 提供 线程安全的上下文传播
└── 桥接 MCP Java SDK session
```

## 问题诊断

### 原设计的问题
1. **层次混淆**：错误地假设 MCP Plus Session 应该依赖 MCP Java SDK Session
2. **职责不清**：没有明确区分协议层和应用层的职责
3. **扩展方向错误**：试图在应用层直接操作协议层对象

### 正确的架构理解
1. **MCP Java SDK Session**：纯协议层，管理连接和消息传输
2. **Spring AI ToolContext**：工具执行层，提供基础上下文支持
3. **MCP Plus Context/Session**：应用层，提供业务级别的增强功能

## 修正后的设计

### 1. 层次分离
```java
EnhancedMcpServerSession {
    - McpServerSession mcpServerSession;     // 协议层
    - McpPlusSession applicationSession;     // 应用层
    - 协议级属性 protocolAttributes;         // 协议相关
    - 应用级属性 applicationAttributes;      // 业务相关
}
```

### 2. 上下文桥接
```java
McpPlusContextBridge {
    // 从不同层次创建增强上下文
    + fromToolContext(ToolContext)
    + fromMcpServerSession(McpServerSession)  
    + fromBothContexts(ToolContext, McpServerSession)
    
    // 向不同层次提取上下文
    + toToolContext(McpPlusContext)
    + toMcpServerSession(McpPlusContext)
}
```

### 3. 职责清晰化

#### MCP Java SDK 层职责：
- JSON-RPC 协议处理
- Transport 层管理
- 连接生命周期
- 协议状态维护

#### Spring AI 层职责：
- 工具定义和执行
- 基础上下文传递
- Tool call 历史记录
- 类型安全的参数传递

#### MCP Plus 层职责：
- 业务会话管理
- 认证和授权
- 请求元数据访问
- 中间件支持
- 高级上下文传播

## 集成策略

### 1. 非侵入式扩展
```java
// 不修改现有接口，通过装饰器模式增强
public class ToolCallbackDecorator implements ToolCallback {
    private final ToolCallback delegate;
    private final MiddlewareChain middlewareChain;
    
    @Override
    public String call(String toolInput, ToolContext toolContext) {
        // 创建增强上下文
        McpPlusContext enhancedContext = McpPlusContextBridge.fromToolContext(toolContext);
        
        // 执行中间件链
        return middlewareChain.execute(toolInput, enhancedContext, () -> {
            return delegate.call(toolInput, toolContext);
        });
    }
}
```

### 2. 渐进式集成
```java
// 阶段1：仅增强 Spring AI ToolContext
McpPlusContext context1 = McpPlusContextBridge.fromToolContext(toolContext);

// 阶段2：集成 MCP Java SDK Session  
McpPlusContext context2 = McpPlusContextBridge.fromMcpServerSession(mcpSession);

// 阶段3：完整集成
McpPlusContext context3 = McpPlusContextBridge.fromBothContexts(toolContext, mcpSession);
```

### 3. 向后兼容性
```java
// 现有的 Spring AI 工具无需修改
@Tool("getUserProfile")
public UserProfile getUserProfile(String userId, ToolContext context) {
    // 仍然可以正常工作
    return userService.getProfile(userId);
}

// 新的增强工具可以使用 MCP Plus 功能
@Tool("getUserProfileEnhanced")
public UserProfile getUserProfileEnhanced(String userId) {
    // 通过静态访问器获取增强上下文
    McpPlusContext context = McpPlusContextAccessor.getCurrentContext();
    String authToken = context.getAuthToken().orElseThrow();
    return userService.getProfileWithAuth(userId, authToken);
}
```

## 实施路径

### Phase 1: 基础增强 (已完成)
- [x] McpPlusContext 接口设计
- [x] McpPlusSession 实现
- [x] 基础上下文传播
- [x] Spring Boot 自动配置

### Phase 2: 协议层集成 (当前)
- [x] EnhancedMcpServerSession 设计
- [x] McpPlusContextBridge 实现
- [ ] MCP Java SDK 集成测试
- [ ] 协议层属性管理

### Phase 3: 高级功能
- [ ] 中间件框架
- [ ] 服务器组合
- [ ] 代理服务器
- [ ] OpenAPI 集成

## 架构优势

### 1. 清晰的分层
- 每层职责明确，不相互侵犯
- 协议层关注通信，应用层关注业务
- 易于理解、维护和扩展

### 2. 渐进式采用
- 可以逐步从 Spring AI ToolContext 迁移
- 不破坏现有代码
- 新功能可选择性使用

### 3. 完整的集成
- 在需要时可以访问所有层次的上下文
- 协议层和应用层信息都可获取
- 支持复杂的企业级需求

这个修正后的架构更好地反映了各层的实际职责，并提供了清晰的集成路径。
