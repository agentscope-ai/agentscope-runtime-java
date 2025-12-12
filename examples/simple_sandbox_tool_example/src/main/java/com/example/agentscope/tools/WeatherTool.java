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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Weather query tool
 * Provide city weather lookup using mock data
 */
@Component
public class WeatherTool {

    private final Random random = new Random();
    
    // Mock weather database
    private final Map<String, String[]> weatherDatabase = new HashMap<>() {{
        put("Beijing", new String[]{"Sunny", "Cloudy", "Overcast", "Light rain"});
        put("Shanghai", new String[]{"Cloudy", "Light rain", "Sunny", "Overcast"});
        put("Guangzhou", new String[]{"Sunny", "Cloudy", "Thunderstorm", "Sunny"});
        put("Shenzhen", new String[]{"Cloudy", "Sunny", "Light rain", "Sunny"});
        put("Hangzhou", new String[]{"Overcast", "Light rain", "Cloudy", "Sunny"});
        put("Chengdu", new String[]{"Cloudy", "Overcast", "Light rain", "Cloudy"});
    }};

    /**
     * Get current weather information for a specified city
     *
     * @param city City name (for example: Beijing, Shanghai, Guangzhou)
     * @return Weather info string
     */
    @Tool(description = "Get current weather for a specified city, including condition and temperature")
    public String getWeather(
            @ToolParam(name = "city", description = "City name, for example: Beijing, Shanghai, Guangzhou") 
            String city) {
        
        System.out.println("🌤️  Weather tool invoked: querying weather for " + city);
        
        // Simulate querying weather
        String[] conditions = weatherDatabase.getOrDefault(
            city, 
            new String[]{"Sunny", "Cloudy", "Overcast", "Light rain"}
        );
        
        String condition = conditions[random.nextInt(conditions.length)];
        int temperature = 15 + random.nextInt(20); // 15-35°C
        int humidity = 40 + random.nextInt(40);    // 40-80%
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        
        return String.format(
            "Weather for %s\n" +
            "🕐 Time: %s\n" +
            "☁️ Condition: %s\n" +
            "🌡️ Temperature: %d°C\n" +
            "💧 Humidity: %d%%",
            city, time, condition, temperature, humidity
        );
    }

    /**
     * Get the weather forecast for the coming days
     *
     * @param city City name
     * @param days Number of forecast days (1-7 days)
     * @return Weather forecast details
     */
    @Tool(description = "Get the weather forecast for a specified city for the next few days")
    public String getForecast(
            @ToolParam(name = "city", description = "City name") 
            String city,
            @ToolParam(name = "days", description = "Number of forecast days, range 1-7") 
            int days) {
        
        System.out.println("📅 Forecast tool invoked: querying weather for " + city + " for the next " + days + " days");
        
        if (days < 1 || days > 7) {
            return "Forecast days must be between 1 and 7";
        }
        
        StringBuilder forecast = new StringBuilder();
        forecast.append(String.format("Weather forecast for %s for the next %d days\n", city, days));
        
        String[] conditions = weatherDatabase.getOrDefault(
            city, 
            new String[]{"Sunny", "Cloudy", "Overcast", "Light rain"}
        );
        
        for (int i = 1; i <= days; i++) {
            String condition = conditions[random.nextInt(conditions.length)];
            int tempHigh = 20 + random.nextInt(15);
            int tempLow = 10 + random.nextInt(10);
            
            forecast.append(String.format(
                "\nDay %d: %s, temperature %d~%d°C",
                i, condition, tempLow, tempHigh
            ));
        }
        
        return forecast.toString();
    }
}

