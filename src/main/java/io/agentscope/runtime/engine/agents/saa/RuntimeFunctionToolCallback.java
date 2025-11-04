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
package io.agentscope.runtime.engine.agents.saa;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeFunctionToolCallback<I, O> implements ToolCallback {

	private static final Logger logger = LoggerFactory.getLogger(RuntimeFunctionToolCallback.class);

	private static final ToolCallResultConverter DEFAULT_RESULT_CONVERTER = new DefaultToolCallResultConverter();

	private static final ToolMetadata DEFAULT_TOOL_METADATA = ToolMetadata.builder().build();

	private final ToolDefinition toolDefinition;

	private final ToolMetadata toolMetadata;

	private final Type toolInputType;

	private final SandboxAwareTool<I, O> toolFunction;

	private final ToolCallResultConverter toolCallResultConverter;

	public RuntimeFunctionToolCallback(ToolDefinition toolDefinition, @Nullable ToolMetadata toolMetadata, Type toolInputType,
			SandboxAwareTool<I, O> toolFunction, @Nullable ToolCallResultConverter toolCallResultConverter) {
		Assert.notNull(toolDefinition, "toolDefinition cannot be null");
		Assert.notNull(toolInputType, "toolInputType cannot be null");
		Assert.notNull(toolFunction, "toolFunction cannot be null");
		this.toolDefinition = toolDefinition;
		this.toolMetadata = toolMetadata != null ? toolMetadata : DEFAULT_TOOL_METADATA;
		this.toolFunction = toolFunction;
		this.toolInputType = toolInputType;
		this.toolCallResultConverter = toolCallResultConverter != null ? toolCallResultConverter
				: DEFAULT_RESULT_CONVERTER;
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return this.toolDefinition;
	}

	@Override
	public ToolMetadata getToolMetadata() {
		return this.toolMetadata;
	}

	public SandboxAwareTool<I, O> getToolFunction() {
		return this.toolFunction;
	}

	@Override
	public String call(String toolInput) {
		return call(toolInput, null);
	}

	@Override
	public String call(String toolInput, @Nullable ToolContext toolContext) {
		Assert.hasText(toolInput, "toolInput cannot be null or empty");

		logger.debug("Starting execution of tool: {}", this.toolDefinition.name());

		I request = JsonParser.fromJson(toolInput, this.toolInputType);
		O response = this.toolFunction.apply(request, toolContext);

		logger.debug("Successful execution of tool: {}", this.toolDefinition.name());

		return this.toolCallResultConverter.convert(response, null);
	}

	@Override
	public String toString() {
		return "FunctionToolCallback{" + "toolDefinition=" + this.toolDefinition + ", toolMetadata=" + this.toolMetadata
				+ '}';
	}

	/**
	 * Build a {@link RuntimeFunctionToolCallback} from a {@link BiFunction}.
	 */
	public static <I, O> RuntimeFunctionToolCallback.Builder<I, O> builder(String name, SandboxAwareTool<I, O> function) {
		return new RuntimeFunctionToolCallback.Builder<>(name, function);
	}


	public static final class Builder<I, O> {

		private String name;

		private String description;

		private String inputSchema;

		private Type inputType;

		private ToolMetadata toolMetadata;

		private SandboxAwareTool<I, O> toolFunction;

		private ToolCallResultConverter toolCallResultConverter;

		private Builder(String name, SandboxAwareTool<I, O> toolFunction) {
			Assert.hasText(name, "name cannot be null or empty");
			Assert.notNull(toolFunction, "toolFunction cannot be null");
			this.name = name;
			this.toolFunction = toolFunction;
		}

		public RuntimeFunctionToolCallback.Builder<I, O> description(String description) {
			this.description = description;
			return this;
		}

		public RuntimeFunctionToolCallback.Builder<I, O> inputSchema(String inputSchema) {
			this.inputSchema = inputSchema;
			return this;
		}

		public RuntimeFunctionToolCallback.Builder<I, O> inputType(Type inputType) {
			this.inputType = inputType;
			return this;
		}

		public RuntimeFunctionToolCallback.Builder<I, O> inputType(ParameterizedTypeReference<?> inputType) {
			Assert.notNull(inputType, "inputType cannot be null");
			this.inputType = inputType.getType();
			return this;
		}

		public RuntimeFunctionToolCallback.Builder<I, O> toolMetadata(ToolMetadata toolMetadata) {
			this.toolMetadata = toolMetadata;
			return this;
		}

		public RuntimeFunctionToolCallback.Builder<I, O> toolCallResultConverter(ToolCallResultConverter toolCallResultConverter) {
			this.toolCallResultConverter = toolCallResultConverter;
			return this;
		}

		public RuntimeFunctionToolCallback<I, O> build() {
			Assert.notNull(this.inputType, "inputType cannot be null");
			var toolDefinition = DefaultToolDefinition.builder()
					.name(this.name)
					.description(StringUtils.hasText(this.description) ? this.description
							: ToolUtils.getToolDescriptionFromName(this.name))
					.inputSchema(StringUtils.hasText(this.inputSchema) ? this.inputSchema
							: JsonSchemaGenerator.generateForType(this.inputType))
					.build();
			return new RuntimeFunctionToolCallback<>(toolDefinition, this.toolMetadata, this.inputType, this.toolFunction,
					this.toolCallResultConverter);
		}

	}

}
