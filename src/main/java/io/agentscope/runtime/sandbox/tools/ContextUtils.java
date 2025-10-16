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
package io.agentscope.runtime.sandbox.tools;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Utility class for extracting userID and sessionID from ToolContext
 *
 * @author xuehuitian45
 * @since 2025/1/15
 */
public class ContextUtils {

    /**
     * Extract userID from ToolContext
     *
     * @param toolContext tool context
     * @return userID, returns "default_user" if not found
     */
    public static String extractUserID(ToolContext toolContext) {
        OverAllState overAllState = (OverAllState) toolContext.getContext().get("state");
        if (overAllState != null && overAllState.data().containsKey("user_id")) {
            return overAllState.value("user_id", String.class).orElse("default_user");
        }
        return "default_user";
    }

    /**
     * Extract sessionID from ToolContext
     *
     * @param toolContext tool context
     * @return sessionID, returns "default_session" if not found
     */
    public static String extractSessionID(ToolContext toolContext) {
        OverAllState overAllState = (OverAllState) toolContext.getContext().get("state");
        if (overAllState != null && overAllState.data().containsKey("session_id")) {
            return overAllState.value("session_id", String.class).orElse("default_session");
        }
        return "default_session";
    }

    /**
     * Extract both userID and sessionID from ToolContext
     *
     * @param toolContext tool context
     * @return array containing userID and sessionID [userID, sessionID]
     */
    public static String[] extractUserAndSessionID(ToolContext toolContext) {
        return new String[]{extractUserID(toolContext), extractSessionID(toolContext)};
    }
}
