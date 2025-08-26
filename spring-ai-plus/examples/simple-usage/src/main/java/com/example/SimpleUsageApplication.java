package com.example;

import org.springframework.ai.mcp.plus.core.context.McpPlusContext;
import org.springframework.ai.mcp.plus.core.context.McpPlusContextAccessor;
import org.springframework.ai.mcp.plus.core.context.McpPlusSession;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 简单使用示例：展示如何在Spring Boot应用中使用MCP Plus功能
 */
@SpringBootApplication
public class SimpleUsageApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SimpleUsageApplication.class, args);
    }
}

/**
 * 示例服务：展示不同级别的MCP Plus使用
 */
@Component
class ExampleToolService {
    
    /**
     * 基础工具：无需修改现有代码
     */
    @Tool(name = "basicCalculator", description = "Simple calculator")
    public double calculate(@org.springframework.ai.tool.annotation.Parameter("expression") String expression) {
        // 现有工具逻辑保持不变
        return evaluateExpression(expression);
    }
    
    /**
     * 增强工具：使用会话存储计算历史
     */
    @Tool(name = "calculatorWithHistory", description = "Calculator that remembers history")
    public CalculationResult calculateWithHistory(
            @org.springframework.ai.tool.annotation.Parameter("expression") String expression) {
        
        // 获取增强上下文
        McpPlusContext context = McpPlusContextAccessor.getCurrentContext();
        McpPlusSession session = context.getSession();
        
        // 获取计算历史
        List<String> history = session.getData("calculationHistory", List.class)
            .orElse(new ArrayList<>());
        
        // 执行计算
        double result = evaluateExpression(expression);
        
        // 更新历史
        history.add(expression + " = " + result);
        session.setData("calculationHistory", history);
        
        return new CalculationResult(result, history.size(), expression);
    }
    
    /**
     * 认证工具：需要用户认证
     */
    @Tool(name = "secureCalculator", description = "Calculator requiring authentication")
    public CalculationResult secureCalculate(
            @org.springframework.ai.tool.annotation.Parameter("expression") String expression) {
        
        McpPlusContext context = McpPlusContextAccessor.getCurrentContext();
        
        // 检查认证
        String authToken = context.getAuthToken()
            .orElseThrow(() -> new SecurityException("Authentication required"));
        
        // 简单的token验证（实际应用中应该更严格）
        if (!"valid-token".equals(authToken)) {
            throw new SecurityException("Invalid authentication token");
        }
        
        // 获取客户端信息
        String clientId = context.getClientId().orElse("unknown");
        
        // 执行计算
        double result = evaluateExpression(expression);
        
        // 记录审计日志
        McpPlusSession session = context.getSession();
        List<String> auditLog = session.getData("auditLog", List.class)
            .orElse(new ArrayList<>());
        auditLog.add("Client " + clientId + " calculated: " + expression + " = " + result);
        session.setData("auditLog", auditLog);
        
        return new CalculationResult(result, auditLog.size(), expression);
    }
    
    /**
     * 会话管理工具：展示会话操作
     */
    @Tool(name = "sessionManager", description = "Manage session data")
    public SessionInfo manageSession(
            @org.springframework.ai.tool.annotation.Parameter("action") String action,
            @org.springframework.ai.tool.annotation.Parameter("key") String key,
            @org.springframework.ai.tool.annotation.Parameter("value") String value) {
        
        McpPlusContext context = McpPlusContextAccessor.getCurrentContext();
        McpPlusSession session = context.getSession();
        
        switch (action.toLowerCase()) {
            case "set":
                session.setData(key, value);
                return new SessionInfo("SET", key, value, session.getAllData().size());
                
            case "get":
                Optional<String> data = session.getData(key, String.class);
                return new SessionInfo("GET", key, data.orElse(null), session.getAllData().size());
                
            case "remove":
                session.removeData(key);
                return new SessionInfo("REMOVE", key, null, session.getAllData().size());
                
            case "clear":
                int sizeBefore = session.getAllData().size();
                session.getAllData().clear();
                return new SessionInfo("CLEAR", null, null, 0);
                
            case "info":
                return new SessionInfo("INFO", 
                    session.getId(), 
                    "Created: " + session.getCreatedAt() + ", Last Access: " + session.getLastAccessedAt(), 
                    session.getAllData().size());
                
            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
    }
    
    private double evaluateExpression(String expression) {
        // 简单的表达式计算（实际应用中应使用更强大的解析器）
        try {
            // 这里使用简单的eval逻辑，实际应用中需要更安全的实现
            if (expression.contains("+")) {
                String[] parts = expression.split("\\+");
                return Double.parseDouble(parts[0].trim()) + Double.parseDouble(parts[1].trim());
            } else if (expression.contains("-")) {
                String[] parts = expression.split("-");
                return Double.parseDouble(parts[0].trim()) - Double.parseDouble(parts[1].trim());
            } else if (expression.contains("*")) {
                String[] parts = expression.split("\\*");
                return Double.parseDouble(parts[0].trim()) * Double.parseDouble(parts[1].trim());
            } else if (expression.contains("/")) {
                String[] parts = expression.split("/");
                return Double.parseDouble(parts[0].trim()) / Double.parseDouble(parts[1].trim());
            } else {
                return Double.parseDouble(expression.trim());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid expression: " + expression);
        }
    }
}

/**
 * REST控制器：展示HTTP请求集成
 */
@RestController
@RequestMapping("/api")
class ExampleController {
    
    private final ExampleToolService toolService;
    
    public ExampleController(ExampleToolService toolService) {
        this.toolService = toolService;
    }
    
    /**
     * 基础计算接口
     */
    @PostMapping("/calculate")
    public CalculationResult calculate(@RequestBody CalculationRequest request) {
        return toolService.calculateWithHistory(request.getExpression());
    }
    
    /**
     * 认证计算接口
     */
    @PostMapping("/secure-calculate")
    public CalculationResult secureCalculate(
            @RequestBody CalculationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authToken,
            @RequestHeader(value = "X-Client-ID", required = false) String clientId) {
        
        // 创建带认证信息的上下文
        McpPlusContext context = createContextWithAuth(authToken, clientId);
        
        // 在上下文中执行工具
        return McpPlusContextAccessor.runWithContext(context, () -> {
            return toolService.secureCalculate(request.getExpression());
        });
    }
    
    /**
     * 会话管理接口
     */
    @PostMapping("/session/{action}")
    public SessionInfo manageSession(
            @PathVariable String action,
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String value) {
        
        return toolService.manageSession(action, key, value);
    }
    
    private McpPlusContext createContextWithAuth(String authToken, String clientId) {
        // 在实际应用中，应该使用McpPlusContextBridge或适当的工厂方法
        // 这里简化处理
        McpPlusContext context = McpPlusContext.empty();
        if (authToken != null) {
            context.setAttribute("authToken", authToken);
        }
        if (clientId != null) {
            context.setAttribute("clientId", clientId);
        }
        return context;
    }
}

/**
 * 数据传输对象
 */
class CalculationRequest {
    private String expression;
    
    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }
}

class CalculationResult {
    private final double result;
    private final int historyCount;
    private final String expression;
    
    public CalculationResult(double result, int historyCount, String expression) {
        this.result = result;
        this.historyCount = historyCount;
        this.expression = expression;
    }
    
    public double getResult() { return result; }
    public int getHistoryCount() { return historyCount; }
    public String getExpression() { return expression; }
}

class SessionInfo {
    private final String action;
    private final String key;
    private final String value;
    private final int sessionSize;
    
    public SessionInfo(String action, String key, String value, int sessionSize) {
        this.action = action;
        this.key = key;
        this.value = value;
        this.sessionSize = sessionSize;
    }
    
    public String getAction() { return action; }
    public String getKey() { return key; }
    public String getValue() { return value; }
    public int getSessionSize() { return sessionSize; }
}
