# MCP Context Capabilities: FastMCP vs Spring AI Comparison

This document compares the context capabilities between FastMCP 2.0 and Spring AI's MCP implementation, focusing on developer experience (DX) and feature parity.

## Overview

FastMCP 2.0 provides a comprehensive `Context` object that gives tools access to MCP session capabilities like logging, progress reporting, resource access, and more. Spring AI achieves similar functionality through its `ToolContext` mechanism combined with MCP Exchange objects and the `spring-ai-autoconfigure-mcp-plus` enhancements.

**References:**
- [FastMCP 2.0 Overview](https://gofastmcp.com/getting-started/welcome)
- [FastMCP Server Context](https://gofastmcp.com/servers/context)

## Feature Comparison Matrix

| Feature | FastMCP 2.0 | Spring AI + java-sdk-0.10.0 | DX Rating |
|---------|-------------|------------------------------|-----------|
| Logging | ✅ Direct `ctx.info/debug/warn/error` | ✅ Via `McpExchange.loggingNotification` | FastMCP: High, Spring AI: Medium |
| LLM Sampling | ✅ `ctx.sample(...)` | ✅ Via `McpExchange.createMessage` | FastMCP: High, Spring AI: Medium |
| Progress Reporting | ✅ `ctx.report_progress(...)` | ❌ No standard API (workaround via logging) | FastMCP: High, Spring AI: Low |
| Resource Access | ✅ `ctx.read_resource(uri)` | ❌ No equivalent (server-side resource reading) | FastMCP: High, Spring AI: Low |
| Session Info | ✅ `ctx.request_id/client_id/session_id` | ✅ Via Exchange + ThreadLocal session | FastMCP: High, Spring AI: Medium |
| HTTP Headers | ✅ `get_http_headers()` | ❌ No built-in (custom implementation needed) | FastMCP: High, Spring AI: Low |
| HTTP Request | ✅ `get_http_request()` | ❌ No built-in (custom implementation needed) | FastMCP: High, Spring AI: Low |
| State Management | ✅ `ctx.set_state/get_state` | ✅ Via `ToolContext` Map | FastMCP: High, Spring AI: Medium |
| User Elicitation | ✅ `ctx.elicit(...)` | ❌ No equivalent | FastMCP: High, Spring AI: None |
| Change Notifications | ✅ Auto + manual `ctx.send_*_list_changed` | ✅ Auto via Boot Starter config | FastMCP: High, Spring AI: High |

## Detailed Feature Comparison with Code Examples

### 1. Logging

**FastMCP:**
```python
from fastmcp import FastMCP, Context

mcp = FastMCP("Demo Server")

@mcp.tool
async def process_data(data: str, ctx: Context) -> str:
    """Process data with logging."""
    await ctx.debug("Starting data processing")
    await ctx.info(f"Processing {len(data)} characters")
    await ctx.warning("Using deprecated algorithm")
    
    if not data:
        await ctx.error("Empty data provided")
        return "Error: No data"
    
    return f"Processed: {data.upper()}"
```

**Spring AI:**
```java
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import io.modelcontextprotocol.spec.McpSchema;

@Component
public class LoggingTools {
    
    @Tool(description = "Process data with MCP logging")
    public String processData(String data, ToolContext ctx) {
        var exchange = McpToolUtils.getMcpExchange(ctx).orElse(null);
        
        if (exchange != null) {
            // Debug logging
            var debugMsg = new McpSchema.LoggingMessageNotification(
                McpSchema.LoggingLevel.DEBUG, "Starting data processing", null, null);
            exchange.loggingNotification(debugMsg).block();
            
            // Info logging
            var infoMsg = new McpSchema.LoggingMessageNotification(
                McpSchema.LoggingLevel.INFO, "Processing " + data.length() + " characters", null, null);
            exchange.loggingNotification(infoMsg).block();
            
            // Warning logging
            var warnMsg = new McpSchema.LoggingMessageNotification(
                McpSchema.LoggingLevel.WARNING, "Using deprecated algorithm", null, null);
            exchange.loggingNotification(warnMsg).block();
            
            if (data.isEmpty()) {
                var errorMsg = new McpSchema.LoggingMessageNotification(
                    McpSchema.LoggingLevel.ERROR, "Empty data provided", null, null);
                exchange.loggingNotification(errorMsg).block();
                return "Error: No data";
            }
        }
        
        return "Processed: " + data.toUpperCase();
    }
}
```

### 2. LLM Sampling

**FastMCP:**
```python
from fastmcp import FastMCP, Context

mcp = FastMCP("Sampling Demo")

@mcp.tool
async def analyze_text(text: str, ctx: Context) -> str:
    """Analyze text using client's LLM."""
    prompt = f"Please analyze this text and provide insights: {text}"
    response = await ctx.sample(prompt, temperature=0.7, max_tokens=200)
    return response
```

**Spring AI:**
```java
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import io.modelcontextprotocol.spec.McpSchema;
import static io.modelcontextprotocol.spec.McpSchema.Role.*;

@Component
public class SamplingTools {
    
    @Tool(description = "Analyze text using client's LLM")
    public String analyzeText(String text, ToolContext ctx) {
        var exchange = McpToolUtils.getMcpExchange(ctx).orElseThrow(
            () -> new RuntimeException("MCP Exchange not available"));
        
        var userMessage = new McpSchema.PromptMessage(USER, 
            new McpSchema.TextContent("Please analyze this text and provide insights: " + text));
        
        var request = new McpSchema.CreateMessageRequest(
            "analysis", 
            List.of(userMessage), 
            null, // modelPreferences
            null  // systemPrompt
        );
        
        var result = exchange.createMessage(request).block();
        return result != null ? result.output().toString() : "No response";
    }
}
```

### 3. Progress Reporting

**FastMCP:**
```python
from fastmcp import FastMCP, Context
import asyncio

mcp = FastMCP("Progress Demo")

@mcp.tool
async def long_running_task(items: int, ctx: Context) -> str:
    """Perform a long-running task with progress updates."""
    for i in range(items):
        await asyncio.sleep(0.1)  # Simulate work
        await ctx.report_progress(progress=i+1, total=items)
    
    return f"Completed processing {items} items"
```

**Spring AI:**
```java
// Note: Spring AI doesn't have built-in progress reporting API
// Workaround using logging notifications

@Component
public class ProgressTools {
    
    @Tool(description = "Long running task with progress (via logging)")
    public String longRunningTask(int items, ToolContext ctx) throws InterruptedException {
        var exchange = McpToolUtils.getMcpExchange(ctx).orElse(null);
        
        for (int i = 0; i < items; i++) {
            Thread.sleep(100); // Simulate work
            
            if (exchange != null) {
                int progress = ((i + 1) * 100) / items;
                var progressMsg = new McpSchema.LoggingMessageNotification(
                    McpSchema.LoggingLevel.INFO, 
                    "Progress: " + progress + "% (" + (i + 1) + "/" + items + ")", 
                    null, null);
                exchange.loggingNotification(progressMsg).block();
            }
        }
        
        return "Completed processing " + items + " items";
    }
}
```

### 4. Session and Request Information

**FastMCP:**
```python
from fastmcp import FastMCP, Context

mcp = FastMCP("Session Demo")

@mcp.tool
async def get_session_info(ctx: Context) -> dict:
    """Get information about current session and request."""
    return {
        "request_id": ctx.request_id,
        "client_id": ctx.client_id or "Unknown",
        "session_id": ctx.session_id,
        "server_name": ctx.fastmcp.name
    }
```

**Spring AI:**
```java
import org.springframework.ai.mcp.plus.context.SessionThreadLocalContext;
import io.modelcontextprotocol.spec.McpServerSession;

@Component
public class SessionTools {
    
    @Tool(description = "Get session and client information")
    public Map<String, Object> getSessionInfo(ToolContext ctx) {
        var exchange = McpToolUtils.getMcpExchange(ctx).orElse(null);
        var session = SessionThreadLocalContext.get().orElse(null);
        
        Map<String, Object> info = new HashMap<>();
        info.put("client_name", exchange != null && exchange.getClientInfo() != null ? 
            exchange.getClientInfo().name() : "unknown");
        info.put("session_id", session != null ? session.getId() : "n/a");
        info.put("client_capabilities", exchange != null && exchange.getClientCapabilities() != null ?
            exchange.getClientCapabilities().toString() : "unknown");
        
        return info;
    }
}
```

### 5. HTTP Headers Access

**FastMCP:**
```python
from fastmcp import FastMCP, Context
from fastmcp.server.dependencies import get_http_headers

mcp = FastMCP("HTTP Demo")

@mcp.tool
async def get_user_agent() -> dict:
    """Get HTTP request information."""
    headers = get_http_headers()
    return {
        "user_agent": headers.get("user-agent", "Unknown"),
        "content_type": headers.get("content-type", "Unknown"),
        "authorization": "Bearer" if headers.get("authorization", "").startswith("Bearer") else "None"
    }
```

**Spring AI (Custom Implementation):**
```java
// 1. ThreadLocal for HTTP headers
public final class HttpHeadersHolder {
    private static final ThreadLocal<Map<String, String>> TL = new ThreadLocal<>();
    
    public static void set(HttpHeaders headers) {
        Map<String, String> headerMap = new HashMap<>();
        headers.forEach((key, values) -> headerMap.put(key.toLowerCase(), String.join(",", values)));
        TL.set(headerMap);
    }
    
    public static Map<String, String> get() {
        return Optional.ofNullable(TL.get()).orElse(Map.of());
    }
    
    public static void clear() {
        TL.remove();
    }
}

// 2. Custom Transport Provider with header capture
@Component
@Primary
public class HeaderCapturingTransportProvider extends WebMvcSseServerTransportProviderPlus {
    
    public HeaderCapturingTransportProvider(ObjectMapper objectMapper, 
                                          McpServerProperties serverProperties) {
        super(objectMapper, serverProperties.getBaseUrl(),
              serverProperties.getSseMessageEndpoint(), 
              serverProperties.getSseEndpoint());
    }
    
    @Override
    protected ServerResponse handleMessage(ServerRequest request) {
        // Capture headers before processing
        HttpHeadersHolder.set(request.headers().asHttpHeaders());
        try {
            return super.handleMessage(request);
        } finally {
            HttpHeadersHolder.clear();
        }
    }
}

// 3. ToolContextEnricher to inject headers
@Component
public class HttpHeadersEnricher implements ToolContextEnricher {
    
    @Override
    public boolean supports(ToolContext ctx) {
        return true;
    }
    
    @Override
    public Map<String, Object> enrich(ToolContext ctx) {
        return Map.of("http.headers", HttpHeadersHolder.get());
    }
}

// 4. Tool using headers
@Component
public class HttpTools {
    
    @Tool(description = "Get HTTP request headers")
    public Map<String, Object> getUserAgent(ToolContext ctx) {
        @SuppressWarnings("unchecked")
        var headers = (Map<String, String>) ctx.getContext().get("http.headers");
        
        if (headers == null) {
            return Map.of("error", "No HTTP headers available");
        }
        
        return Map.of(
            "user_agent", headers.getOrDefault("user-agent", "Unknown"),
            "content_type", headers.getOrDefault("content-type", "Unknown"),
            "authorization", headers.getOrDefault("authorization", "").startsWith("Bearer") ? "Bearer" : "None"
        );
    }
}
```

### 6. State Management

**FastMCP:**
```python
from fastmcp import FastMCP, Context
from fastmcp.server.middleware import Middleware, MiddlewareContext

mcp = FastMCP("State Demo")

class UserMiddleware(Middleware):
    async def on_call_tool(self, context: MiddlewareContext, call_next):
        # Set user info in context state
        context.fastmcp_context.set_state("user_id", "user_123")
        context.fastmcp_context.set_state("permissions", ["read", "write"])
        return await call_next()

mcp.add_middleware(UserMiddleware())

@mcp.tool
async def secure_operation(data: str, ctx: Context) -> str:
    """Tool accessing middleware-set state."""
    user_id = ctx.get_state("user_id")
    permissions = ctx.get_state("permissions")
    
    if "write" not in permissions:
        return "Access denied"
    
    return f"Processing {data} for user {user_id}"
```

**Spring AI:**
```java
// Using ToolContext for state management
@Component
public class UserContextEnricher implements ToolContextEnricher {
    
    @Override
    public boolean supports(ToolContext ctx) {
        return true;
    }
    
    @Override
    public Map<String, Object> enrich(ToolContext ctx) {
        // Simulate user context (in real app, extract from JWT/session)
        return Map.of(
            "user_id", "user_123",
            "permissions", List.of("read", "write"),
            "tenant_id", "tenant_456"
        );
    }
}

@Component
public class StateTools {
    
    @Tool(description = "Secure operation using context state")
    public String secureOperation(String data, ToolContext ctx) {
        var userId = (String) ctx.getContext().get("user_id");
        @SuppressWarnings("unchecked")
        var permissions = (List<String>) ctx.getContext().get("permissions");
        
        if (permissions == null || !permissions.contains("write")) {
            return "Access denied";
        }
        
        return "Processing " + data + " for user " + userId;
    }
}
```

### 7. User Elicitation

**FastMCP:**
```python
from fastmcp import FastMCP, Context

mcp = FastMCP("Elicitation Demo")

@mcp.tool
async def interactive_setup(ctx: Context) -> str:
    """Interactive tool that asks user for input."""
    # Ask for user's name
    name_result = await ctx.elicit("What is your name?", response_type=str)
    if name_result.action != "accept":
        return "Setup cancelled"
    
    name = name_result.data
    
    # Ask for age with validation
    age_result = await ctx.elicit("What is your age?", response_type=int)
    if age_result.action != "accept":
        return "Setup cancelled"
    
    age = age_result.data
    
    return f"Hello {name}, you are {age} years old!"
```

**Spring AI:**
```java
// Note: Spring AI/MCP Java SDK doesn't have equivalent elicitation API
// This is a significant gap in functionality

@Component
public class InteractionTools {
    
    @Tool(description = "Interactive setup (limited - no elicitation API)")
    public String interactiveSetup(String name, Integer age, ToolContext ctx) {
        // Workaround: require all parameters upfront
        // Cannot do progressive disclosure like FastMCP
        
        if (name == null || name.trim().isEmpty()) {
            return "Error: Name is required";
        }
        
        if (age == null || age < 0) {
            return "Error: Valid age is required";
        }
        
        return String.format("Hello %s, you are %d years old!", name, age);
    }
    
    // Alternative: Use LLM sampling to simulate interaction (not equivalent)
    @Tool(description = "Simulate interaction via LLM sampling")
    public String simulateInteraction(ToolContext ctx) {
        var exchange = McpToolUtils.getMcpExchange(ctx).orElse(null);
        if (exchange == null) {
            return "No MCP exchange available";
        }
        
        var prompt = new McpSchema.PromptMessage(
            McpSchema.Role.USER,
            new McpSchema.TextContent("Please ask the user for their name and age, then respond with a greeting.")
        );
        
        var request = new McpSchema.CreateMessageRequest("interaction", List.of(prompt), null, null);
        var result = exchange.createMessage(request).block();
        
        return result != null ? result.output().toString() : "No response";
    }
}
```

## Configuration Examples

### FastMCP Server Setup

```python
from fastmcp import FastMCP

mcp = FastMCP(
    name="Demo Server",
    version="1.0.0"
)

# Tools are automatically registered via decorators
@mcp.tool
async def my_tool(param: str, ctx: Context) -> str:
    await ctx.info(f"Processing: {param}")
    return f"Result: {param.upper()}"

if __name__ == "__main__":
    mcp.run()  # Starts server with all capabilities
```

### Spring AI MCP Server Setup

```yaml
# application.yml
spring:
  ai:
    mcp:
      server:
        name: demo-server
        version: 1.0.0
        type: SYNC
        capabilities:
          tool: true
          resource: true
          prompt: true
          completion: true
        tool-change-notification: true
        resource-change-notification: true
        prompt-change-notification: true
```

```java
@SpringBootApplication
public class McpServerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
    
    @Bean
    public ToolCallbackProvider demoTools(MyToolService toolService) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(toolService)
            .build();
    }
}

@Service
public class MyToolService {
    
    @Tool(description = "Demo tool with context")
    public String myTool(String param, ToolContext ctx) {
        var exchange = McpToolUtils.getMcpExchange(ctx).orElse(null);
        if (exchange != null) {
            var msg = new McpSchema.LoggingMessageNotification(
                McpSchema.LoggingLevel.INFO, "Processing: " + param, null, null);
            exchange.loggingNotification(msg).block();
        }
        return "Result: " + param.toUpperCase();
    }
}
```

## Summary

### Strengths of FastMCP 2.0
- **Excellent DX**: Simple `@mcp.tool` decorator with automatic context injection
- **Rich Context API**: Comprehensive `ctx` object with all MCP capabilities
- **HTTP Integration**: Built-in access to HTTP request/headers via dependency functions
- **User Elicitation**: Interactive tool execution with structured user input
- **Progress Reporting**: Native progress updates to clients
- **Resource Self-Access**: Tools can read their own server's resources

### Strengths of Spring AI + MCP Plus
- **Enterprise Integration**: Leverages Spring ecosystem (Security, Boot, etc.)
- **Production Ready**: Built-in observability, configuration, deployment features
- **Type Safety**: Strong typing with compile-time validation
- **Flexible Architecture**: Can integrate with existing Spring applications
- **Custom Extensions**: Your `mcp-plus` module demonstrates extensibility

### Key Gaps in Spring AI
1. **No User Elicitation**: Missing `ctx.elicit()` equivalent
2. **No Progress API**: No standard progress reporting mechanism
3. **No Resource Self-Access**: Tools cannot easily read server's own resources
4. **HTTP Context**: Requires custom implementation for request/header access
5. **Verbose Logging**: More boilerplate compared to FastMCP's `ctx.info()`

### Recommendations

For Spring AI to achieve FastMCP-level DX, consider:

1. **Enhanced ToolContext**: Add direct logging methods (`ctx.info()`, `ctx.debug()`)
2. **Progress API**: Implement `ctx.reportProgress(current, total)`
3. **HTTP Integration**: Built-in HTTP request/header access in ToolContext
4. **Resource Access API**: `ctx.readResource(uri)` for server-side resource reading
5. **Elicitation Support**: Interactive tool execution capabilities

The `spring-ai-autoconfigure-mcp-plus` module already demonstrates the right approach with ThreadLocal session propagation and context enrichment. These patterns could be extended to cover the remaining gaps.
