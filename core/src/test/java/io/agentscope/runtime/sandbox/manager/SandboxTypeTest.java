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

package io.agentscope.runtime.sandbox.manager;


import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit tests for {@link SandboxType}
 */
class SandboxTypeTest {

    @ParameterizedTest
    @DisplayName("Should return true for valid predefined field names")
    @ValueSource(strings = {
            "BASE",
            "BROWSER",
            "FILESYSTEM",
            "TRAINING",
            "APPWORLD",
            "BFCL",
            "WEBSHOP",
            "GUI",
            "MOBILE",
            "CUSTOM",
            "AGENTBAY"
    })
    void shouldReturnTrueForValidFieldNames(String typeName) {
        assertTrue(SandboxType.isPredefinedType(typeName),
                "Should recognize " + typeName + " as a predefined field name");
    }

    @ParameterizedTest
    @DisplayName("Should return false for invalid or unknown names")
    @ValueSource(strings = {
            "UNKNOWN",
            "docker",
            "PYTHON",
            "123",
            " "
    })
    void shouldReturnFalseForInvalidNames(String typeName) {
        assertFalse(SandboxType.isPredefinedType(typeName),
                "Should not recognize " + typeName + " as a predefined field name");
    }

    @Test
    @DisplayName("Should return false for null input")
    void shouldReturnFalseForNull() {
        assertFalse(SandboxType.isPredefinedType(null));
    }

    @Test
    @DisplayName("Should return false for empty string")
    void shouldReturnFalseForEmptyString() {
        assertFalse(SandboxType.isPredefinedType(""));
    }

    /**
     * Note: Based on the current implementation using field.getName(),
     * lowercase values (the actual constants) will return false.
     */
    @ParameterizedTest
    @DisplayName("Current implementation check: lowercase values should return false")
    @ValueSource(strings = {
            "base",
            "browser",
            "filesystem"
    })
    void shouldReturnFalseForFieldValues(String typeValue) {
        assertFalse(SandboxType.isPredefinedType(typeValue),
                "Currently implementation checks field names (BASE), not values (base)");
    }
}