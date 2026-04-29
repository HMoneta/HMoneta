package fan.summer.hmoneta.service.log;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class SseLogService {

    private static final Map<String, Set<SseEmitter>> serviceEmitters = new ConcurrentHashMap<>();
    private static final Set<SseEmitter> allLogEmitters = new CopyOnWriteArraySet<>();

    public SseEmitter subscribe(String serviceName) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        if (serviceName == null || serviceName.isEmpty() || "*".equals(serviceName)) {
            allLogEmitters.add(emitter);
        } else {
            serviceEmitters.computeIfAbsent(serviceName, k -> new CopyOnWriteArraySet<>()).add(emitter);
        }

        emitter.onCompletion(() -> removeEmitter(serviceName, emitter));
        emitter.onTimeout(() -> removeEmitter(serviceName, emitter));
        emitter.onError(e -> removeEmitter(serviceName, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("subscribed"));
        } catch (IOException e) {
            removeEmitter(serviceName, emitter);
        }

        return emitter;
    }

    private void removeEmitter(String serviceName, SseEmitter emitter) {
        if (serviceName == null || serviceName.isEmpty() || "*".equals(serviceName)) {
            allLogEmitters.remove(emitter);
        } else {
            Set<SseEmitter> emitters = serviceEmitters.get(serviceName);
            if (emitters != null) {
                emitters.remove(emitter);
            }
        }
    }

    public void broadcast(String serviceName, String jsonMessage) {
        for (SseEmitter emitter : allLogEmitters) {
            sendToEmitter(emitter, jsonMessage);
        }

        if (serviceName != null && !serviceName.isEmpty()) {
            Set<SseEmitter> emitters = serviceEmitters.get(serviceName);
            if (emitters != null) {
                for (SseEmitter emitter : emitters) {
                    sendToEmitter(emitter, jsonMessage);
                }
            }
        }
    }

    private void sendToEmitter(SseEmitter emitter, String data) {
        try {
            emitter.send(SseEmitter.event().data(data));
        } catch (IOException e) {
            emitter.complete();
            removeEmitterFromAllSets(emitter);
        } catch (IllegalStateException e) {
            removeEmitterFromAllSets(emitter);
        }
    }

    private void removeEmitterFromAllSets(SseEmitter emitter) {
        allLogEmitters.remove(emitter);
        for (Set<SseEmitter> set : serviceEmitters.values()) {
            set.remove(emitter);
        }
    }
}
