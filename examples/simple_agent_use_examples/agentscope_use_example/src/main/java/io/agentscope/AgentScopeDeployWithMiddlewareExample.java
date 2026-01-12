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

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import io.agentscope.runtime.app.AgentApp;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.web.servlet.FilterRegistrationBean;

public class AgentScopeDeployWithMiddlewareExample {

	private static final Logger LOG = LoggerFactory.getLogger(AgentScopeDeployWithMiddlewareExample.class);

	public static void main(String[] args) {
		String[] commandLine = new String[2];
		commandLine[0] = "-f";
		commandLine[1] = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(".env")).getPath();
		AgentApp app = new AgentApp(commandLine);
		FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
		filterRegistrationBean.setFilter(new MyFilter());
		filterRegistrationBean.setBeanName("myFilter");
		filterRegistrationBean.setUrlPatterns(List.of("/test/get"));
		filterRegistrationBean.setOrder(-1);
		app.middleware(filterRegistrationBean);
		app.run();
	}



	public static class MyFilter implements Filter{

		@Override
		public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
			HttpServletRequest request = (HttpServletRequest) servletRequest;
			LOG.info("Current request uri = {}",request.getRequestURI());
			filterChain.doFilter(servletRequest,servletResponse);
		}
	}
}
