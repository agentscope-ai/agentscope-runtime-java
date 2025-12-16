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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic sandbox type that allows runtime registration of custom types
 */
public class DynamicSandboxType {
    private static final Map<String, DynamicSandboxType> CUSTOM_TYPES = new ConcurrentHashMap<>();

    public static final DynamicSandboxType BASE = fromEnum(SandboxType.BASE);
    public static final DynamicSandboxType BROWSER = fromEnum(SandboxType.BROWSER);
    public static final DynamicSandboxType FILESYSTEM = fromEnum(SandboxType.FILESYSTEM);
    public static final DynamicSandboxType TRAINING = fromEnum(SandboxType.TRAINING);
    public static final DynamicSandboxType APPWORLD = fromEnum(SandboxType.APPWORLD);
    public static final DynamicSandboxType BFCL = fromEnum(SandboxType.BFCL);
    public static final DynamicSandboxType WEBSHOP = fromEnum(SandboxType.WEBSHOP);
    public static final DynamicSandboxType GUI = fromEnum(SandboxType.GUI);
    public static final DynamicSandboxType MOBILE = fromEnum(SandboxType.MOBILE);
    public static final DynamicSandboxType AGENTBAY = fromEnum(SandboxType.AGENTBAY);

    private final String typeName;
    private final SandboxType enumType;

    private DynamicSandboxType(String typeName, SandboxType enumType) {
        this.typeName = typeName;
        this.enumType = enumType;
    }

    public static DynamicSandboxType fromEnum(SandboxType sandboxType) {
        return new DynamicSandboxType(sandboxType.getTypeName(), sandboxType);
    }

    public static DynamicSandboxType custom(String typeName) {
        if (typeName == null || typeName.trim().isEmpty()) {
            throw new IllegalArgumentException("Type name cannot be null or empty");
        }

        String normalizedName = typeName.toLowerCase();

        try {
            SandboxType enumType = SandboxType.valueOf(typeName.toUpperCase());
            return fromEnum(enumType);
        } catch (IllegalArgumentException e) {
            // Not a predefined type, create or get custom type
        }

        return CUSTOM_TYPES.computeIfAbsent(normalizedName, name -> new DynamicSandboxType(name, null));
    }

    public static DynamicSandboxType valueOf(String typeName) {
        if (typeName == null || typeName.trim().isEmpty()) {
            throw new IllegalArgumentException("Type name cannot be null or empty");
        }

        try {
            SandboxType enumType = SandboxType.valueOf(typeName.toUpperCase());
            return fromEnum(enumType);
        } catch (IllegalArgumentException e) {
            String normalizedName = typeName.toLowerCase();
            DynamicSandboxType customType = CUSTOM_TYPES.get(normalizedName);
            if (customType != null) {
                return customType;
            }
            throw new IllegalArgumentException("Unknown sandbox type: " + typeName);
        }
    }

    public boolean isEnum() {
        return enumType != null;
    }

    public boolean isCustom() {
        return enumType == null;
    }

    public SandboxType getEnumType() {
        return enumType;
    }

    public String getTypeName() {
        return typeName;
    }

    public SandboxType toEnum() {
        if (enumType == null) {
            throw new UnsupportedOperationException("Cannot convert custom type '" + typeName + "' to SandboxType enum");
        }
        return enumType;
    }

    public static Map<String, DynamicSandboxType> getCustomTypes() {
        return new ConcurrentHashMap<>(CUSTOM_TYPES);
    }

    public static boolean removeCustomType(String typeName) {
        if (typeName == null) {
            return false;
        }
        String normalizedName = typeName.toLowerCase();
        return CUSTOM_TYPES.remove(normalizedName) != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamicSandboxType that = (DynamicSandboxType) o;
        return Objects.equals(typeName, that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName);
    }

    @Override
    public String toString() {
        return "DynamicSandboxType{" + "typeName='" + typeName + '\'' + ", isCustom=" + isCustom() + '}';
    }
}

