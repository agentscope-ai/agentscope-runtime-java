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
package io.agentscope.runtime.sandbox.box;

import io.agentscope.runtime.sandbox.box.model.HostPrerequisiteError;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.fs.FileSystemConfig;
import io.agentscope.runtime.sandbox.manager.fs.local.LocalFileSystemConfig;
import io.agentscope.runtime.sandbox.manager.registry.RegisterSandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RegisterSandbox(
        imageName = "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-mobile:latest",
        sandboxType = "mobile",
        securityLevel = "high",
        timeout = 60,
        description = "Mobile Sandbox",
        runtimeConfig = {"privileged=true"}
)
public class MobileSandbox extends Sandbox {
    private static final Logger logger = LoggerFactory.getLogger(MobileSandbox.class);
    private static boolean hostCheckDone = false;

    public MobileSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId
    ) {
        this(managerApi, userId, sessionId, Map.of());
    }

    public MobileSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            FileSystemConfig fileSystemConfig
    ) {
        this(managerApi, userId, sessionId, fileSystemConfig, Map.of());
    }

    public MobileSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            Map<String, String> environment
    ) {
        this(managerApi, userId, sessionId, LocalFileSystemConfig.builder().build(), environment);
    }

    public MobileSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            FileSystemConfig fileSystemConfig,
            Map<String, String> environment) {
        super(managerApi, userId, sessionId, "mobile");
        if (!hostCheckDone) {
            checkHostReadiness();
            hostCheckDone = true;
        }
    }

    /**
     * Performs a one-time check of the host environment to ensure
     * all prerequisites for MobileSandbox are met.
     *
     * @throws HostPrerequisiteError if host prerequisites are not met
     */
    public void checkHostReadiness() {
        logger.info("Performing one-time host environment check for MobileSandbox...");

        String arch = System.getProperty("os.arch").toLowerCase();

        if ("aarch64".equals(arch) || "arm64".equals(arch)) {
            logger.warn("""
                    
                    ======================== WARNING ========================
                    ARM64/aarch64 architecture detected (e.g., Apple M-series).
                    Running this mobile sandbox on a non-x86_64 host may lead\s
                     to unexpected compatibility or performance issues.
                    =========================================================""");
        }

        String osName = System.getProperty("os.name");
        if (osName.contains("Linux")) {
            List<String> requiredDevices = List.of(
                    "/dev/binder",
                    "/dev/hwbinder",
                    "/dev/vndbinder",
                    "/dev/ashmem"
            );

            List<String> missingDevices = new ArrayList<>();
            for (String device : requiredDevices) {
                if (!new File(device).exists()) {
                    missingDevices.add(device);
                }
            }

            if (!missingDevices.isEmpty()) {
                String errorMessage = String.format("""
                                
                                ========== HOST PREREQUISITE FAILED ==========
                                MobileSandbox requires specific kernel modules on the host machine.
                                The following required device files are missing:
                                  - %s
                                
                                To fix this, please run the following commands on your Linux host:
                                
                                1. Install extra kernel modules:
                                   sudo apt update && sudo apt install -y linux-modules-extra-`uname -r`
                                
                                2. Load modules and create device nodes:
                                   sudo modprobe binder_linux devices="binder,hwbinder,vndbinder"
                                   sudo modprobe ashmem_linux
                                
                                After running these commands, verify with:
                                   ls -l /dev/binder* /dev/ashmem
                                ==================================================""",
                        String.join(", ", missingDevices));
                throw new HostPrerequisiteError(errorMessage);
            }
        }

        logger.info("Host environment check passed.");
    }

    /**
     * A general-purpose method to execute various ADB actions.
     * This function acts as a low-level dispatcher for different ADB commands.
     * Only the parameters relevant to the specified action should be provided.
     * For actions involving coordinates, the values are absolute pixels,
     * with the origin (0, 0) at the top-left of the screen.
     *
     * @param action     The specific ADB action to perform.
     *                   Examples: "tap", "swipe", "input_text", "key_event",
     *                   "get_screenshot", "get_screen_resolution"
     * @param coordinate The [x, y] coordinates for a "tap" action
     * @param start      The starting [x, y] coordinates for a "swipe" action
     * @param end        The ending [x, y] coordinates for a "swipe" action
     * @param duration   The duration of a "swipe" gesture in milliseconds
     * @param code       The key event code (e.g., 3) or name (e.g., "HOME") for the "key_event" action
     * @param text       The text string to be entered for the "input_text" action
     * @return The result of the ADB action
     */
    public String adbUse(
            String action,
            List<Integer> coordinate,
            List<Integer> start,
            List<Integer> end,
            Integer duration,
            Object code,
            String text) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        if (coordinate != null) {
            payload.put("coordinate", coordinate);
        }
        if (start != null) {
            payload.put("start", start);
        }
        if (end != null) {
            payload.put("end", end);
        }
        if (duration != null) {
            payload.put("duration", duration);
        }
        if (code != null) {
            payload.put("code", code);
        }
        if (text != null) {
            payload.put("text", text);
        }

        return callTool("adb", payload);
    }

    /**
     * Get the screen resolution of the connected mobile device.
     *
     * @return The screen resolution information
     */
    public String mobileGetScreenResolution() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("action", "get_screen_resolution");
        return callTool("adb", arguments);
    }

    /**
     * Tap a specific coordinate on the screen.
     *
     * @param x The x-coordinate in pixels from the left edge
     * @param y The y-coordinate in pixels from the top edge
     * @return The result of the tap action
     */
    public String mobileTap(int x, int y) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("action", "tap");
        arguments.put("coordinate", List.of(x, y));
        return callTool("adb", arguments);
    }

    /**
     * Perform a swipe gesture on the screen from a start point to an end point.
     *
     * @param start    The starting coordinates [x, y] in pixels
     * @param end      The ending coordinates [x, y] in pixels
     * @param duration The duration of the swipe in milliseconds (optional)
     * @return The result of the swipe action
     */
    public String mobileSwipe(List<Integer> start, List<Integer> end, Integer duration) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("action", "swipe");
        arguments.put("start", start);
        arguments.put("end", end);
        if (duration != null) {
            arguments.put("duration", duration);
        }
        return callTool("adb", arguments);
    }

    /**
     * Input a text string into the currently focused UI element.
     *
     * @param text The string to be inputted
     * @return The result of the input action
     */
    public String mobileInputText(String text) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("action", "input_text");
        arguments.put("text", text);
        return callTool("adb", arguments);
    }

    /**
     * Send an Android key event to the device.
     *
     * @param code The key event code (e.g., 3 for HOME) or a string representation (e.g., "HOME", "BACK")
     * @return The result of the key event action
     */
    public String mobileKeyEvent(Object code) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("action", "key_event");
        arguments.put("code", code);
        return callTool("adb", arguments);
    }

    /**
     * Take a screenshot of the current device screen.
     *
     * @return The screenshot data
     */
    public String mobileGetScreenshot() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("action", "get_screenshot");
        return callTool("adb", arguments);
    }
}
