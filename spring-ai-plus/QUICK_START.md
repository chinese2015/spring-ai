# Spring AI MCP Plus 快速入门

## 5分钟快速体验

### 1. 添加依赖 (30秒)

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-plus-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 写一个简单工具 (2分钟)

```java
@Component
public class MyToolService {
    
    // 普通工具 - 无需修改，自动获得增强功能
    @Tool(name = "hello", description = "Say hello")
    public String hello(@Parameter("name") String name) {
        return "Hello, " + name + "!";
    }
    
    // 增强工具 - 使用会话存储
    @Tool(name = "remember", description = "Remember and greet")
    public String remember(@Parameter("name") String name) {
        // 获取当前上下文和会话
        McpPlusContext context = McpPlusContextAccessor.getCurrentContext();
        McpPlusSession session = context.getSession();
        
        // 从会话获取之前记住的名字
        List<String> names = session.getData("names", List.class)
            .orElse(new ArrayList<>());
        
        if (!names.contains(name)) {
            names.add(name);
            session.setData("names", names);
            return "Nice to meet you, " + name + "! I'll remember you.";
        } else {
            return "Welcome back, " + name + "! I remember you from before.";
        }
    }
}
```

### 3. 配置应用 (30秒)

```yaml
spring:
  ai:
    mcp:
      plus:
        enabled: true  # 默认已启用
```

### 4. 启动应用 (30秒)

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### 5. 测试功能 (1分钟)

创建一个简单的控制器来测试：

```java
@RestController
public class TestController {
    
    @Autowired
    private MyToolService toolService;
    
    @GetMapping("/hello/{name}")
    public String hello(@PathVariable String name) {
        return toolService.hello(name);
    }
    
    @GetMapping("/remember/{name}")
    public String remember(@PathVariable String name) {
        return toolService.remember(name);
    }
}
```

启动应用后，访问：
- `http://localhost:8080/hello/World` → `Hello, World!`
- `http://localhost:8080/remember/Alice` → `Nice to meet you, Alice! I'll remember you.`
- `http://localhost:8080/remember/Alice` → `Welcome back, Alice! I remember you from before.`

## 就这么简单！

你的现有Spring AI工具无需任何修改就自动获得了：
- ✅ **会话管理** - 在多次调用间保持状态
- ✅ **上下文传播** - 线程安全的上下文传递
- ✅ **认证支持** - 从HTTP头自动提取认证信息
- ✅ **请求元数据** - 访问传输类型、时间戳等信息

## 进阶功能预览

### 认证工具
```java
@Tool(name = "secureAction", description = "Secure action")
public String secureAction(@Parameter("data") String data) {
    String token = McpPlusContextAccessor.getAuthToken()
        .orElseThrow(() -> new SecurityException("Login required"));
    return "Processed: " + data + " (authenticated)";
}
```

### HTTP集成
```java
@PostMapping("/secure")
public String secure(@RequestBody String data, 
                    @RequestHeader("Authorization") String token) {
    
    McpPlusContext context = McpPlusContext.empty();
    context.setAttribute("authToken", token);
    
    return McpPlusContextAccessor.runWithContext(context, () -> {
        return toolService.secureAction(data);
    });
}
```

### MCP Server Session集成
```java
public void handleMcpRequest(Object mcpServerSession, String input) {
    McpPlusContext context = McpPlusContextBridge
        .fromMcpServerSession(mcpServerSession);
    
    McpPlusContextAccessor.runWithContext(context, () -> {
        // 所有工具都能访问MCP会话信息
        return toolService.processInput(input);
    });
}
```

## 下一步

1. 查看 [完整使用指南](USAGE_GUIDE.md)
2. 运行 [示例项目](examples/)
3. 探索 [高级功能](detailed_design.md)

**恭喜！** 你已经掌握了Spring AI MCP Plus的基础用法！🎉
