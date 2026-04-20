package se.lexicon.flightbooking_api.dto;

public record AiChatRequest(
        String sessionId,
        String message
) {}