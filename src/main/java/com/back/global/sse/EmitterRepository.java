package com.back.global.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmitterRepository {

    private final Map<Long, Map<String, SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter save(Long memberId, String emitterId, SseEmitter emitter) {
        emitters.computeIfAbsent(memberId, id -> new ConcurrentHashMap<>())
                .put(emitterId, emitter);
        return emitter;
    }

    public Map<String, SseEmitter> findEmittersByMemberId(Long memberId) {
        return emitters.getOrDefault(memberId, new ConcurrentHashMap<>());
    }

    public void deleteEmitter(Long memberId, String emitterId) {
        Map<String, SseEmitter> memberEmitters = emitters.get(memberId);
        if (memberEmitters != null) {
            memberEmitters.remove(emitterId);
        }
    }
}
