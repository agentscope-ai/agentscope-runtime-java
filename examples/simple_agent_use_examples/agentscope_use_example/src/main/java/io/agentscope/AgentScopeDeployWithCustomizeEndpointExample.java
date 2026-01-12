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

package io.agentscope;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import io.agentscope.runtime.app.AgentApp;

import org.springframework.web.servlet.function.ServerResponse;

public class AgentScopeDeployWithCustomizeEndpointExample {

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


	public static void main(String[] args) {
		String[] commandLine = new String[2];
		commandLine[0] = "-f";
		commandLine[1] = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(".env")).getPath();
		AgentApp app = new AgentApp(commandLine);
		app.endpoint("/test/post", List.of("POST"),serverRequest -> {
			String time = sdf.format(new Date());
			return ServerResponse.ok().body(String.format("time=%s,post successful",time));
		});
		app.endpoint("/test/get", List.of("GET"),serverRequest -> {
			String time = sdf.format(new Date());
			return ServerResponse.ok().body(String.format("time=%s,get successful",time));
		});
		app.run();
	}
}
