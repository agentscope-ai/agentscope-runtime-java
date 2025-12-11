package com.example.agentscope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope Spring Boot application main class
 */
@SpringBootApplication
public class AgentScopeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentScopeApplication.class, args);
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🎉 AgentScope Demo started successfully!");
        System.out.println("=".repeat(70));
        System.out.println();
        
        System.out.println("📡 API endpoints:");
        System.out.println("   • Health check: http://localhost:8080/api/chat/health");
        System.out.println("   • Send message: http://localhost:8080/api/chat");
        System.out.println("   • View tools: http://localhost:8080/api/chat/tools");
        System.out.println("   • Reset conversation: http://localhost:8080/api/chat/reset");
        System.out.println();
        
        System.out.println("🛠️  Available tools:");
        System.out.println("   • Weather tool - query city weather and forecasts");
        System.out.println("   • Calculator tool - math operations (add, subtract, multiply, divide, power, sqrt)");
        System.out.println("   • Sandbox tool - sandbox browser search");
        System.out.println();
        
        System.out.println("💡 Quick tests:");
        System.out.println("   curl -X POST http://localhost:8080/api/chat \\");
        System.out.println("     -H \"Content-Type: application/json\" \\");
        System.out.println("     -d '{\"message\": \"How is the weather in Beijing today?\"}'");
        System.out.println();

        System.out.println("curl -X POST http://localhost:8080/api/chat \\\n" +
                "     -H \"Content-Type: application/json\" \\\n" +
                "     -d '{\"message\": \"Use the browser tool to search on Baidu for today'\\''s gold price\"}'");
        
        System.out.println("   curl -X POST http://localhost:8080/api/chat \\");
        System.out.println("     -H \"Content-Type: application/json\" \\");
        System.out.println("     -d '{\"message\": \"Calculate 123 + 456\"}'");
        System.out.println();
        
        System.out.println("📚 More info:");
        System.out.println("   • See README.md for detailed docs");
        System.out.println("   • See QUICKSTART.md to get started quickly");
        System.out.println("   • Import AgentScope-API.postman_collection.json to test with Postman");
        System.out.println();
        System.out.println("=".repeat(70) + "\n");
    }
}

