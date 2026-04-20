package se.lexicon.flightbooking_api.ai;

public record ChatMessage(
        String role,   // "user" or "assistant"
        String content
) {}