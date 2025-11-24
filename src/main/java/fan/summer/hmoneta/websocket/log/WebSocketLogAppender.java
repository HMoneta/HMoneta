package fan.summer.hmoneta.websocket.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 类的详细说明
 *
 * @author summer
 * @version 1.00
 * @Date 2025/11/3
 */
public class WebSocketLogAppender extends AppenderBase<ILoggingEvent> {
    // Service名称 -> 订阅的Sessions
    private static final Map<String, Set<WebSocketSession>> serviceSubscriptions = new ConcurrentHashMap<>();

    // 订阅了所有日志的Sessions
    private static final Set<WebSocketSession> allLogsSessions = new CopyOnWriteArraySet<>();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 订阅特定Service的日志
     */
    public static void subscribeService(String serviceName, WebSocketSession session) {
        serviceSubscriptions.computeIfAbsent(serviceName, k -> new CopyOnWriteArraySet<>())
                .add(session);
        System.out.println("Session " + session.getId() + " 订阅了 " + serviceName);
    }

    /**
     * 订阅所有日志
     */
    public static void subscribeAll(WebSocketSession session) {
        allLogsSessions.add(session);
        System.out.println("Session " + session.getId() + " 订阅了所有日志");
    }

    /**
     * 取消订阅
     */
    public static void unsubscribe(String serviceName, WebSocketSession session) {
        Set<WebSocketSession> sessions = serviceSubscriptions.get(serviceName);
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    /**
     * 移除Session的所有订阅
     */
    public static void removeSession(WebSocketSession session) {
        allLogsSessions.remove(session);
        for (Set<WebSocketSession> sessions : serviceSubscriptions.values()) {
            sessions.remove(session);
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        try {
            // 获取Logger名称，通常是类的全限定名
            String loggerName = event.getLoggerName();

            // 提取Service名称 (例如: com.example.service.UserService -> UserService)
            String serviceName = extractServiceName(loggerName);

            // 构造日志消息
            Map<String, Object> logMessage = new HashMap<>();
            logMessage.put("timestamp", event.getTimeStamp());
            logMessage.put("level", event.getLevel().toString());
            logMessage.put("service", serviceName);
            logMessage.put("thread", event.getThreadName());
            logMessage.put("message", event.getFormattedMessage());

            // 如果有异常信息
            if (event.getThrowableProxy() != null) {
                logMessage.put("exception", event.getThrowableProxy().getMessage());
            }

            String jsonMessage = objectMapper.writeValueAsString(logMessage);
            TextMessage textMessage = new TextMessage(jsonMessage);

            // 1. 发送给订阅了该Service的客户端
            Set<WebSocketSession> serviceSessions = serviceSubscriptions.get(serviceName);
            if (serviceSessions != null) {
                sendToSessions(serviceSessions, textMessage);
            }

            // 2. 发送给订阅了所有日志的客户端
            sendToSessions(allLogsSessions, textMessage);

        } catch (Exception e) {
            addError("Failed to append log", e);
        }
    }

    /**
     * 从Logger名称提取Service名称
     * com.example.service.UserService -> UserService
     * com.example.service.impl.OrderServiceImpl -> OrderService
     */
    private String extractServiceName(String loggerName) {
        String[] parts = loggerName.split("\\.");
        String className = parts[parts.length - 1];

        // 移除 Impl 后缀
        if (className.endsWith("Impl")) {
            className = className.substring(0, className.length() - 4);
        }

        return className;
    }

    /**
     * 发送消息到多个Session
     */
    private void sendToSessions(Set<WebSocketSession> sessions, TextMessage message) {
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                }
            } catch (Exception e) {
                addError("Failed to send message to session " + session.getId(), e);
            }
        }
    }
}
