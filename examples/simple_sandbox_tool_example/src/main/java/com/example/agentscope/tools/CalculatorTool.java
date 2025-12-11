/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.agentscope.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Calculator tool
 * Provide basic mathematical operations
 */
@Component
public class CalculatorTool {

    /**
     * Add two numbers
     */
    @Tool(description = "Calculate the sum of two numbers")
    public double add(
            @ToolParam(name = "a", description = "First number") double a,
            @ToolParam(name = "b", description = "Second number") double b) {
        
        System.out.println("➕ Calculator invoked: " + a + " + " + b);
        return a + b;
    }

    /**
     * Subtract two numbers
     */
    @Tool(description = "Calculate the difference of two numbers")
    public double subtract(
            @ToolParam(name = "a", description = "Minuend") double a,
            @ToolParam(name = "b", description = "Subtrahend") double b) {
        
        System.out.println("➖ Calculator invoked: " + a + " - " + b);
        return a - b;
    }

    /**
     * Multiply two numbers
     */
    @Tool(description = "Calculate the product of two numbers")
    public double multiply(
            @ToolParam(name = "a", description = "First number") double a,
            @ToolParam(name = "b", description = "Second number") double b) {
        
        System.out.println("✖️ Calculator invoked: " + a + " × " + b);
        return a * b;
    }

    /**
     * Divide two numbers
     */
    @Tool(description = "Calculate the quotient of two numbers")
    public double divide(
            @ToolParam(name = "a", description = "Dividend") double a,
            @ToolParam(name = "b", description = "Divisor") double b) {
        
        System.out.println("➗ Calculator invoked: " + a + " ÷ " + b);
        
        if (b == 0) {
            throw new IllegalArgumentException("Divisor cannot be zero");
        }
        return a / b;
    }

    /**
     * Calculate exponentiation
     */
    @Tool(description = "Calculate a number raised to a power")
    public double power(
            @ToolParam(name = "base", description = "Base") double base,
            @ToolParam(name = "exponent", description = "Exponent") double exponent) {
        
        System.out.println("🔢 Calculator invoked: " + base + " ^ " + exponent);
        return Math.pow(base, exponent);
    }

    /**
     * Calculate square root
     */
    @Tool(description = "Calculate the square root of a number")
    public double sqrt(
            @ToolParam(name = "number", description = "Number to calculate the square root of") double number) {
        
        System.out.println("√ Calculator invoked: √" + number);
        
        if (number < 0) {
            throw new IllegalArgumentException("Cannot calculate the square root of a negative number");
        }
        return Math.sqrt(number);
    }
}

