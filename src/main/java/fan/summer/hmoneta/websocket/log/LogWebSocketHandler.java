package fan.summer.hmoneta.websocket.log;


import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * 类的详细说明
 *
 * @author summer
 * @version 1.00
 * @Date 2025/11/3
 */
@Component
public class LogWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket连接建立: " + session.getId());

        // 发送欢迎消息
        Map<String, String> welcome = new HashMap<>();
        welcome.put("type", "connected");
        welcome.put("message", "日志WebSocket连接成功");
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcome)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode json = objectMapper.readTree(payload);

        String action = json.get("action").asText();

        switch (action) {
            case "subscribe":
                handleSubscribe(session, json);
                break;
            case "unsubscribe":
                handleUnsubscribe(session, json);
                break;
            case "subscribeAll":
                handleSubscribeAll(session);
                break;
            case "ping":
                // 直接回 pong，不需要 error
                Map<String, String> pong = new HashMap<>();
                pong.put("type", "pong");
                pong.put("timestamp", String.valueOf(System.currentTimeMillis()));
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
                break;
            default:
                sendError(session, "未知的操作: " + action);
        }
    }

    /**
     * 订阅特定Service
     */
    private void handleSubscribe(WebSocketSession session, JsonNode json) throws Exception {
        String serviceName = json.get("service").asText();
        WebSocketLogAppender.subscribeService(serviceName, session);

        Map<String, String> response = new HashMap<>();
        response.put("type", "subscribed");
        response.put("service", serviceName);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    /**
     * 取消订阅
     */
    private void handleUnsubscribe(WebSocketSession session, JsonNode json) throws Exception {
        String serviceName = json.get("service").asText();
        WebSocketLogAppender.unsubscribe(serviceName, session);

        Map<String, String> response = new HashMap<>();
        response.put("type", "unsubscribed");
        response.put("service", serviceName);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    /**
     * 订阅所有日志
     */
    private void handleSubscribeAll(WebSocketSession session) throws Exception {
        WebSocketLogAppender.subscribeAll(session);

        Map<String, String> response = new HashMap<>();
        response.put("type", "subscribed");
        response.put("service", "ALL");
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    /**
     * 发送错误消息
     */
    private void sendError(WebSocketSession session, String errorMessage) throws Exception {
        Map<String, String> error = new HashMap<>();
        error.put("type", "error");
        error.put("message", errorMessage);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("WebSocket连接关闭: " + session.getId());
        WebSocketLogAppender.removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket传输错误: " + exception.getMessage());
        WebSocketLogAppender.removeSession(session);
    }
}
