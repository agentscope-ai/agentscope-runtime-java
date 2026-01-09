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
package io.agentscope.runtime.sandbox.manager.remote;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that can be executed both locally and remotely.
 * When a SandboxService is configured with a remote base URL, methods annotated
 * with @RemoteWrapper will automatically make HTTP requests to the remote server
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteWrapper {
    
    /**
     * HTTP method to use for the remote call
     */
    RequestMethod method() default RequestMethod.POST;
    
    /**
     * Key in the response JSON that contains the actual data
     * If empty, the whole response will be returned
     */
    String successKey() default "data";
    
    /**
     * Custom path for the endpoint. If empty, uses the method name
     */
    String path() default "";
}

