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

import React, { useState, useRef, useEffect } from "react";
import { Layout, Input, Avatar, Spin, Typography, Card } from "antd";
import type { InputRef } from "antd";
import { SendOutlined, RobotOutlined, UserOutlined } from "@ant-design/icons";
import ReactMarkdown from "react-markdown";
import "./App.css";

const { Content } = Layout;
const { Text } = Typography;

const REACT_APP_API_URL =
  process.env.REACT_APP_API_URL || "http://localhost:8080";
const BACKEND_URL = REACT_APP_API_URL + "/v1/chat/completions";
const BACKEND_WS_URL = REACT_APP_API_URL + "/env_info";
const DEFAULT_MODEL = "qwen-max";
const systemMessage = {
  role: "system",
  content: "You are a helpful assistant.",
};

type SiteItem = {
  title: string;
  url: string;
  favicon: string;
  description: string;
};
type ChatMessage = {
  message: string;
  think: string;
  sender: string;
  site: SiteItem[];
  status?: string;
  toolName?: string;
  toolId?: string;
  toolInput?: string;
  toolResult?: string;
}[];

const App: React.FC = () => {
  const inputRef = useRef<InputRef>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const [vncUrl, setVncUrl] = useState("");
  const handleFocus = () => {
    if (inputRef.current) {
      inputRef.current.select();
    }
  };
    const [messages, setMessages] = useState<ChatMessage>([
    {
      message: "Hello, I'm the assistant! Ask me anything!",
      sender: "assistant",
      think: "",
      site: [],
      status: undefined,
      toolName: undefined,
      toolId: undefined,
      toolInput: undefined,
      toolResult: undefined,
    },
  ]);
  const [isTyping, setIsTyping] = useState(false);

  async function getVncInfo() {
    const response = await fetch(BACKEND_WS_URL, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    console.log(data);
    if (data.baseUrl && data.runtimeToken) {
      const baseVncPath = data.baseUrl.replace("/fastapi", "/vnc/vnc_lite.html");
      console.log(baseVncPath);
      const encodedPassword = encodeURIComponent(data.runtimeToken);
      const vncUrl = `${baseVncPath}?password=${encodedPassword}`;

      // 添加重试机制,确保 VNC 服务就绪
      await retryUntilVncReady(vncUrl);
      setVncUrl(vncUrl);
    }
  }

  // 轮询检查 VNC 服务是否就绪
  async function retryUntilVncReady(url: string, maxRetries = 5, delay = 1000) {
    for (let i = 0; i < maxRetries; i++) {
      try {
        const response = await fetch(url, { method: "HEAD" });
        if (response.ok) {
          console.log(`VNC service ready after ${i + 1} attempt(s)`);
          return true;
        }
      } catch (error) {
        console.log(`VNC not ready, retrying... (${i + 1}/${maxRetries})`);
      }

      if (i < maxRetries - 1) {
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }

    console.warn("VNC service may not be fully ready, proceeding anyway");
    return false;
  }


  const handleSend = async (message: string) => {
    await getVncInfo();
    if (message.trim() === "") {
      return;
    }
    const newMessage = {
      message,
      sender: "user",
      think: "",
      site: [],
      status: undefined,
      toolName: undefined,
      toolId: undefined,
      toolInput: undefined,
      toolResult: undefined,
    };

    const newMessages = [...messages, newMessage];

    setMessages(newMessages);

    setIsTyping(true);
    await processMessageToChatGPT(newMessages);
  };

  async function processMessageToChatGPT(chatMessages: ChatMessage) {
    let apiMessages = chatMessages
      .map((messageObject) => {
        if (messageObject.message.trim() === "") {
          return null;
        }
        let role = messageObject.sender === "assistant" ? "assistant" : "user";
        return { role, content: messageObject.message };
      })
      .filter(Boolean);

    const apiRequestBody = {
      model: DEFAULT_MODEL,
      messages: [systemMessage, ...apiMessages],
      stream: true,
    };

    const response = await fetch(BACKEND_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(apiRequestBody),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    if (!response.body) {
      throw new Error("ReadableStream not found in response.");
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let accumulatedMessage = "";
    setMessages([
      ...chatMessages,
      {
        message: "",
        sender: "assistant",
        think: "",
        site: [],
        status: undefined,
        toolName: undefined,
        toolId: undefined,
        toolInput: undefined,
        toolResult: undefined,
      },
    ]);
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      const chunk = decoder.decode(value);
      accumulatedMessage += chunk;

      const lines = accumulatedMessage.split("\n");
      accumulatedMessage = lines.pop() || "";

      for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed || !trimmed.startsWith("data:")) continue;

        // 部分后端会重复前缀 "data:"，用正则剥离所有连续的 data:
        const payload = trimmed.replace(/^(data:\s*)+/, "");
        if (!payload || payload === "[DONE]") continue;

        try {
          const parsed = JSON.parse(payload);
          const delta = parsed.choices[0]?.delta || {};
          const content = delta.content || "";
          const messageType = delta.messageType;
          
          if (messageType === "TOOL_CALL" || messageType === "TOOL_RESPONSE") {
            setMessages((prevMessages) => [
              ...prevMessages.slice(0, -1),
              {
                ...prevMessages[prevMessages.length - 1],
                message: prevMessages[prevMessages.length - 1].message,
                sender: "assistant",
                site: [],
                status: messageType,
                toolName: delta.toolName,
                toolId: delta.toolId,
                toolInput: delta.toolInput,
                toolResult: delta.toolResult,
              },
            ]);
          } else if (content) {
            setMessages((prevMessages) => [
              ...prevMessages.slice(0, -1),
              {
                ...prevMessages[prevMessages.length - 1],
                message:
                  prevMessages[prevMessages.length - 1].message + content,
                sender: "assistant",
                site: [],
                status: undefined,
              },
            ]);
          }
        } catch (error) {
          console.error("Error parsing JSON:", error);
        }
      }
    }

    setIsTyping(false);
  }

  useEffect(() => {
    if (listRef.current) {
      const container = listRef.current;
      // 检查用户是否在底部附近（50px 范围内）
      const isNearBottom = 
        container.scrollHeight - container.scrollTop - container.clientHeight < 50;
      
      // 只有在底部附近时才自动滚动到底部
      if (isNearBottom) {
        container.scrollTop = container.scrollHeight;
      }
    }
  }, [messages]);

  return (
    <Layout className="app-layout">
      <Content className="app-content">
        <div className="app-container">
          <div className="chat-section">
            <div className="chat-header">
              <div className="logo-section">
                <img
                  src="logo512.png"
                  alt="Logo"
                  className="app-logo"
                  onClick={() => window.location.reload()}
                />
                <Text className="app-title">AgentScope Browser Assistant</Text>
              </div>
            </div>

            <div className="messages-container" ref={listRef}>
              {messages.map((item, index) => (
                <div
                  key={index}
                  className={`message-wrapper ${
                    item.sender === "user" ? "user-message" : "assistant-message"
                  }`}
                >
                  <div className="message-content">
                    <Avatar
                      className="message-avatar"
                      size={40}
                      icon={
                        item.sender === "user" ? (
                          <UserOutlined />
                        ) : (
                          <RobotOutlined />
                        )
                      }
                      src={
                        item.sender === "user"
                          ? "user_avatar.svg"
                          : "logo512.png"
                      }
                    />
                    <Card className="message-card" variant="outlined">
                      <div className="message-text">
                        {item.sender === "assistant" ? (
                          <ReactMarkdown>{item.message}</ReactMarkdown>
                        ) : (
                          <Text>{item.message}</Text>
                        )}
                        {}
                        {item.status === "TOOL_CALL" && (
                          <div style={{ 
                            marginTop: 8, 
                            padding: 12, 
                            backgroundColor: "#e6f7ff", 
                            borderRadius: 4,
                            color: "#1890ff",
                            fontSize: 14
                          }}>
                            <div style={{ fontWeight: "bold", marginBottom: 8 }}>
                              🔧 Calling Tool: {item.toolName || "Unknown"}
                            </div>
                            {item.toolId && (
                              <div style={{ fontSize: 12, marginBottom: 4, opacity: 0.8 }}>
                                Tool ID: {item.toolId}
                              </div>
                            )}
                            {item.toolInput && (
                              <div style={{ fontSize: 12, marginTop: 8, padding: 8, backgroundColor: "rgba(255,255,255,0.5)", borderRadius: 4 }}>
                                <div style={{ fontWeight: "bold", marginBottom: 4 }}>Input:</div>
                                <pre style={{ margin: 0, whiteSpace: "pre-wrap", wordBreak: "break-word" }}>
                                  {item.toolInput}
                                </pre>
                              </div>
                            )}
                          </div>
                        )}
                        {item.status === "TOOL_RESPONSE" && (
                          <div style={{ 
                            marginTop: 8, 
                            padding: 12, 
                            backgroundColor: "#f6ffed", 
                            borderRadius: 4,
                            color: "#52c41a",
                            fontSize: 14
                          }}>
                            <div style={{ fontWeight: "bold", marginBottom: 8 }}>
                              ✅ Tool Call Complete: {item.toolName || "Unknown"}
                            </div>
                            {item.toolId && (
                              <div style={{ fontSize: 12, marginBottom: 4, opacity: 0.8 }}>
                                Tool ID: {item.toolId}
                              </div>
                            )}
                            {item.toolResult && (
                              <div style={{ fontSize: 12, marginTop: 8, padding: 8, backgroundColor: "rgba(255,255,255,0.5)", borderRadius: 4 }}>
                                <div style={{ fontWeight: "bold", marginBottom: 4 }}>Result:</div>
                                <pre style={{ margin: 0, whiteSpace: "pre-wrap", wordBreak: "break-word" }}>
                                  {item.toolResult}
                                </pre>
                              </div>
                            )}
                          </div>
                        )}
                        {isTyping &&
                          item === messages[messages.length - 1] && (
                            <Spin
                              size="small"
                              style={{ marginLeft: 8 }}
                              indicator={
                                <span className="typing-indicator">
                                  <span></span>
                                  <span></span>
                                  <span></span>
                                </span>
                              }
                            />
                          )}
                      </div>
                    </Card>
                  </div>
                </div>
              ))}
            </div>

            <div className="input-section">
              <Input
                ref={inputRef}
                className="chat-input"
                placeholder="Type your message here..."
                size="large"
                onPressEnter={(e) => {
                  e.preventDefault();
                  handleSend((e.target as HTMLInputElement).value);
                  (e.target as HTMLInputElement).value = "";
                }}
                onFocus={handleFocus}
                suffix={
                  <SendOutlined
                    className="send-icon"
                    onClick={() => {
                      if (inputRef.current?.input?.value) {
                        handleSend(inputRef.current.input.value);
                        inputRef.current.input.value = "";
                      }
                    }}
                  />
                }
              />
            </div>
          </div>

          <div className="browser-section">
            {vncUrl ? (
              <div className="vnc-container">
                <div className="vnc-iframe-wrapper">
                  <iframe
                    src={vncUrl}
                    className="vnc-iframe"
                    title="VNC Browser"
                    allow="clipboard-read; clipboard-write"
                    scrolling="no"
                  />
                </div>
              </div>
            ) : (
              <div className="vnc-placeholder">
                <Text type="secondary">Waiting for browser sandbox to initialize...</Text>
              </div>
            )}
          </div>
        </div>
      </Content>
    </Layout>
  );
};

export default App;
