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
package io.agentscope.runtime.sandbox.manager.model.container;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;

/**
 * Sandbox type enumeration
 */
public class SandboxType {
    public static final String BASE = "base";
    public static final String BROWSER = "browser";
    public static final String FILESYSTEM = "filesystem";
    public static final String TRAINING = "training";
    public static final String APPWORLD = "appworld";
    public static final String BFCL = "bfcl";
    public static final String WEBSHOP = "webshop";
    public static final String GUI = "gui";
    public static final String MOBILE = "mobile";
    public static final String AGENTBAY = "agentbay";

    public static boolean isPredefinedType(String typeName) {
        if (StringUtils.isBlank(typeName)) {
            return false;
        }

        Field[] fields = SandboxType.class.getDeclaredFields();
        for (Field field : fields) {
            if (typeName.equals(field.getName())) {
                return true;
            }
        }
        return false;
    }
}