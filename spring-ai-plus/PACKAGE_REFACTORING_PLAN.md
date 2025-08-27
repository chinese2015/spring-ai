# MCP Plus Core 包结构重构计划

## 当前问题

目前所有类都放在 `org.springframework.ai.mcp.plus.core.context` 包中，违反了Spring Boot包结构最佳实践：

```
context/
├── AdaptiveContextHolder.java
├── DefaultMcpPlusContext.java  
├── DefaultMcpPlusSession.java
├── DefaultRequestMetadata.java
├── DefaultSessionManager.java
├── EnhancedMcpServerSession.java
├── EnvironmentDetector.java
├── McpPlusContext.java
├── McpPlusContextAccessor.java
├── McpPlusContextBridge.java
├── McpPlusContextHolder.java
├── McpPlusSession.java
├── McpPlusSessionManager.java
├── RequestMetadata.java
├── ToolContext.java
└── package-info.java
```

## 新包结构设计

参考Spring AI自身的模块化架构和Spring Boot最佳实践：

```
org.springframework.ai.mcp.plus.core
├── context/                     # 上下文核心接口和API
│   ├── McpPlusContext.java      # 核心上下文接口
│   ├── McpPlusContextAccessor.java  # 静态访问器
│   └── ToolContext.java         # 临时兼容类
├── session/                     # 会话管理
│   ├── McpPlusSession.java      # 会话接口
│   ├── McpPlusSessionManager.java   # 会话管理器接口
│   ├── impl/                    # 会话实现
│   │   ├── DefaultMcpPlusSession.java
│   │   └── DefaultSessionManager.java
│   └── enhanced/                # MCP集成增强
│       └── EnhancedMcpServerSession.java
├── metadata/                    # 请求元数据
│   ├── RequestMetadata.java     # 元数据接口
│   └── impl/
│       └── DefaultRequestMetadata.java
├── holder/                      # 上下文持有者
│   ├── McpPlusContextHolder.java    # 抽象接口
│   └── impl/
│       └── AdaptiveContextHolder.java
├── bridge/                      # 上下文桥接
│   └── McpPlusContextBridge.java
├── environment/                 # 环境检测
│   └── EnvironmentDetector.java
├── impl/                        # 核心实现
│   └── DefaultMcpPlusContext.java
└── support/                     # 支持类和工具
    └── ...
```

## 重构步骤

### 阶段1：创建新包结构
1. 创建新的包目录
2. 移动接口类到对应包
3. 移动实现类到impl子包

### 阶段2：按功能职责分离
1. **context** - 核心上下文接口和访问器
2. **session** - 会话管理相关
3. **metadata** - 请求元数据处理  
4. **holder** - 上下文持有者模式
5. **bridge** - 不同层次上下文桥接
6. **environment** - 环境检测和适配
7. **impl** - 核心实现类
8. **support** - 工具类和辅助功能

### 阶段3：更新导入和测试
1. 更新所有import语句
2. 修复测试类
3. 更新package-info文档

## Spring Boot最佳实践对照

### ✅ 遵循的原则
- **按功能分层**：context, session, metadata等
- **接口与实现分离**：接口在顶层，实现在impl子包
- **单一职责**：每个包负责一个明确的功能域
- **依赖方向正确**：impl依赖接口，不反向依赖

### ✅ Spring AI风格对齐
参考spring-ai-model的结构：
```
org.springframework.ai.model
├── chat/
├── embedding/  
├── function/
└── ...
```

我们的结构：
```
org.springframework.ai.mcp.plus.core
├── context/
├── session/
├── metadata/
└── ...
```

## 迁移影响

### 需要更新的文件
- [ ] 所有Java类的package声明
- [ ] 所有import语句
- [ ] 测试类
- [ ] package-info.java文档
- [ ] 自动配置类中的扫描路径

### 向后兼容性
- 保持public API不变
- 主要是内部包结构调整
- 对使用者透明
