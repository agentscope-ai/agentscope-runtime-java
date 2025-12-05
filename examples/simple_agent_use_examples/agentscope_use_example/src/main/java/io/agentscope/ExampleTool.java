/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

public class ExampleTool {

	/**
	 * Get current time in a specific timezone.
	 *
	 * @param timezone Timezone name (e.g., "Asia/Tokyo", "America/New_York")
	 * @return Current time string
	 */
	@Tool(
			name = "get_current_time",
			description = "Get the current time in a specific timezone")
	public String getCurrentTime(
			@ToolParam(
					name = "timezone",
					description =
							"Timezone name, e.g., 'Asia/Tokyo', 'America/New_York',"
									+ " 'Europe/London'")
			String timezone) {
		System.out.println("getCurrentTime called with timezone: " + timezone);
		try {
			ZoneId zoneId = ZoneId.of(timezone);
			LocalDateTime now = LocalDateTime.now(zoneId);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			return String.format("Current time in %s: %s", timezone, now.format(formatter));
		} catch (Exception e) {
			return "Error: Invalid timezone. Try 'Asia/Tokyo' or 'America/New_York'";
		}
	}

	/**
	 * Calculate simple math expressions.
	 *
	 * @param expression Math expression (e.g., "123 + 456", "10 * 20")
	 * @return Calculation result
	 */
	@Tool(name = "calculate", description = "Calculate simple math expressions")
	public String calculate(
			@ToolParam(
					name = "expression",
					description =
							"Math expression to evaluate, e.g., '123 + 456', '10 * 20'")
			String expression) {

		try {
			// Simple calculator supporting +, -, *, /
			expression = expression.replaceAll("\\s+", "");

			double result;
			if (expression.contains("+")) {
				String[] parts = expression.split("\\+");
				result = Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
			} else if (expression.contains("-")) {
				String[] parts = expression.split("-");
				result = Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
			} else if (expression.contains("*")) {
				String[] parts = expression.split("\\*");
				result = Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
			} else if (expression.contains("/")) {
				String[] parts = expression.split("/");
				result = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
			} else {
				return "Error: Unsupported operation. Use +, -, *, or /";
			}

			return String.format("%s = %.2f", expression, result);

		} catch (Exception e) {
			return "Error: Invalid expression. Example: '123 + 456'";
		}
	}

	/**
	 * Simulate search functionality.
	 *
	 * @param query Search query
	 * @return Simulated search results
	 */
	@Tool(name = "search", description = "Search for information (simulated)")
	public String search(
			@ToolParam(name = "query", description = "Search query") String query) {

		// Simulate search with predefined responses
		String lowerQuery = query.toLowerCase();

		if (lowerQuery.contains("ai") || lowerQuery.contains("artificial intelligence")) {
			return "Search results for 'artificial intelligence':\n"
					+ "1. AI is the simulation of human intelligence by machines\n"
					+ "2. Common AI applications: chatbots, image recognition, autonomous"
					+ " vehicles\n"
					+ "3. Major AI technologies: machine learning, deep learning, NLP";
		} else if (lowerQuery.contains("java")) {
			return "Search results for 'java':\n"
					+ "1. Java is a high-level, object-oriented programming language\n"
					+ "2. First released by Sun Microsystems in 1995\n"
					+ "3. Known for 'Write Once, Run Anywhere' (WORA) philosophy";
		} else if (lowerQuery.contains("agentscope")) {
			return "Search results for 'AgentScope':\n"
					+ "1. AgentScope is an agent-oriented programming framework\n"
					+ "2. Supports building LLM applications with multi-agent systems\n"
					+ "3. Provides transparent and flexible agent development";
		} else {
			return String.format(
					"Search results for '%s':\n"
							+ "1. Result about %s (simulated)\n"
							+ "2. More information on %s (simulated)\n"
							+ "3. Related topics to %s (simulated)",
					query, query, query, query);
		}
	}
}
