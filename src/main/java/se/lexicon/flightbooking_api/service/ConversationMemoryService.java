package se.lexicon.flightbooking_api.service;

import org.springframework.stereotype.Service;
import se.lexicon.flightbooking_api.ai.ChatMessage;

import java.util.*;

@Service
public class ConversationMemoryService {

    private final Map<String, Deque<ChatMessage>> memory = new HashMap<>();
    private static final int MAX_MESSAGES = 10;

    public List<ChatMessage> getMessages(String sessionId) {
        return new ArrayList<>(memory.getOrDefault(sessionId, new ArrayDeque<>()));
    }

    public void addMessage(String sessionId, ChatMessage message) {
        memory.putIfAbsent(sessionId, new ArrayDeque<>());

        Deque<ChatMessage> messages = memory.get(sessionId);
        messages.addLast(message);

        // Keep only last N messages
        if (messages.size() > MAX_MESSAGES) {
            messages.removeFirst();
        }
    }
}