package se.lexicon.flightbooking_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.lexicon.flightbooking_api.ai.ChatMessage;
import se.lexicon.flightbooking_api.ai.OpenAiService;
import se.lexicon.flightbooking_api.dto.AiChatRequest;
import se.lexicon.flightbooking_api.dto.AiChatResponse;
import se.lexicon.flightbooking_api.service.ConversationMemoryService;

import java.util.List;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AiAssistantController {

    private final ConversationMemoryService memoryService;
    private final OpenAiService openAiService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@RequestBody AiChatRequest request) {

        // 1. Save user message
        memoryService.addMessage(request.sessionId(),
                new ChatMessage("user", request.message()));

        // 2. Get conversation history
        List<ChatMessage> history = memoryService.getMessages(request.sessionId());

        // 3. TEMP reply using history
        String reply = openAiService.getChatResponse(history);

        // 4. Save assistant reply
        memoryService.addMessage(request.sessionId(),
                new ChatMessage("assistant", reply));

        return ResponseEntity.ok(new AiChatResponse(reply));
    }
}