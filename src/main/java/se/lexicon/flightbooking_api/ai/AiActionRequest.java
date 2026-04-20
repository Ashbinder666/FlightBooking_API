package se.lexicon.flightbooking_api.ai;

public record AiActionRequest(
        String action,
        Long flightId,
        String destination,
        String name,
        String email
) {}