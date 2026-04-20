package se.lexicon.flightbooking_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.lexicon.flightbooking_api.ai.AiActionRequest;
import se.lexicon.flightbooking_api.ai.ChatMessage;
import se.lexicon.flightbooking_api.ai.OpenAiService;
import se.lexicon.flightbooking_api.dto.AiChatRequest;
import se.lexicon.flightbooking_api.dto.AiChatResponse;
import se.lexicon.flightbooking_api.dto.BookFlightRequestDTO;
import se.lexicon.flightbooking_api.service.ConversationMemoryService;
import se.lexicon.flightbooking_api.service.FlightBookingService;

import java.util.List;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AiAssistantController {

    private final ConversationMemoryService memoryService;
    private final OpenAiService openAiService;
    private final FlightBookingService flightBookingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        memoryService.addMessage(request.sessionId(), new ChatMessage("user", request.message()));

        List<ChatMessage> actionMessages = List.of(
                new ChatMessage("user", request.message())
        );

        String aiJson = openAiService.getChatResponse(actionMessages);

        AiActionRequest action;

        try {
            action = objectMapper.readValue(aiJson, AiActionRequest.class);
        } catch (Exception e) {
            String fallback = "Sorry, I didn't understand that.";
            memoryService.addMessage(request.sessionId(), new ChatMessage("assistant", fallback));
            return ResponseEntity.ok(new AiChatResponse(fallback));
        }

        String reply;
        String actionType = action.action() != null
                ? action.action()
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace('-', '_')
                .replace(' ', '_')
                .toLowerCase()
                : "";

        switch (actionType) {
            case "search_flights", "search" -> {
                try {
                    var flights = flightBookingService.findAvailableFlights();

                    String resultData;
                    if (action.destination() != null && !action.destination().isBlank()) {
                        String destination = action.destination().trim();
                        var matchingFlights = flights.stream()
                                .filter(flight -> flight.destination() != null && flight.destination().equalsIgnoreCase(destination))
                                .toList();

                        if (matchingFlights.isEmpty()) {
                            resultData = "RESULT: No available flights found to " + destination + ".";
                        } else {
                            String flightSummary = matchingFlights.stream()
                                    .limit(5)
                                    .map(flight -> "Flight ID " + flight.id() + " (" + flight.flightNumber() + ") to " + flight.destination() + " departing at " + flight.departureTime() + " costs " + flight.price())
                                    .reduce((a, b) -> a + "; " + b)
                                    .orElse("");

                            resultData = "RESULT: Found " + matchingFlights.size() + " available flights to " + destination + ". " + flightSummary;
                        }
                    } else {
                        if (flights.isEmpty()) {
                            resultData = "RESULT: No available flights found.";
                        } else {
                            String flightSummary = flights.stream()
                                    .limit(5)
                                    .map(flight -> "Flight ID " + flight.id() + " (" + flight.flightNumber() + ") to " + flight.destination() + " departing at " + flight.departureTime() + " costs " + flight.price())
                                    .reduce((a, b) -> a + "; " + b)
                                    .orElse("");

                            resultData = "RESULT: Found " + flights.size() + " available flights. " + flightSummary;
                        }
                    }

                    List<ChatMessage> responseMessages = List.of(
                            new ChatMessage("system", "You are a helpful flight assistant. Explain results clearly to the user."),
                            new ChatMessage("user", resultData)
                    );

                    reply = openAiService.getChatResponse(responseMessages);
                } catch (Exception ex) {
                    List<ChatMessage> responseMessages = List.of(
                            new ChatMessage("system", "You are a helpful flight assistant. Explain errors clearly to the user."),
                            new ChatMessage("user", "ERROR: " + ex.getMessage())
                    );

                    reply = openAiService.getChatResponse(responseMessages);
                }
            }

            case "book_flight", "book" -> {
                try {
                    var booking = flightBookingService.bookFlight(
                            action.flightId(),
                            new BookFlightRequestDTO(action.name(), action.email())
                    );

                    String resultData = "BOOKED: Flight ID " + booking.id() +
                            " (" + booking.flightNumber() + ") to " + booking.destination() +
                            " departing at " + booking.departureTime() +
                            " for passenger " + booking.passengerName();

                    List<ChatMessage> responseMessages = List.of(
                            new ChatMessage("system", "You are a helpful flight assistant. Explain results clearly to the user."),
                            new ChatMessage("user", resultData)
                    );

                    reply = openAiService.getChatResponse(responseMessages);
                } catch (Exception ex) {
                    List<ChatMessage> responseMessages = List.of(
                            new ChatMessage("system", "You are a helpful flight assistant. Explain errors clearly to the user."),
                            new ChatMessage("user", "ERROR: " + ex.getMessage())
                    );

                    reply = openAiService.getChatResponse(responseMessages);
                }
            }

            case "cancel_flight", "cancel", "cancel_booking", "cancel_reservation" -> {
                try {
                    flightBookingService.cancelFlight(action.flightId(), action.email());

                    String resultData = "CANCELLED: Flight with ID " + action.flightId() + " for " + action.email() + " has been cancelled.";

                    List<ChatMessage> responseMessages = List.of(
                            new ChatMessage("system", "You are a helpful flight assistant. Explain results clearly to the user."),
                            new ChatMessage("user", resultData)
                    );

                    reply = openAiService.getChatResponse(responseMessages);
                } catch (Exception ex) {
                    List<ChatMessage> responseMessages = List.of(
                            new ChatMessage("system", "You are a helpful flight assistant. Explain errors clearly to the user."),
                            new ChatMessage("user", "ERROR: " + ex.getMessage())
                    );

                    reply = openAiService.getChatResponse(responseMessages);
                }
            }

            case "get_bookings", "bookings", "show_bookings", "show_booking", "my_bookings" -> {
                try {
                    var bookings = flightBookingService.findBookingsByEmail(action.email());

                    String resultData;
                    if (bookings.isEmpty()) {
                        resultData = "RESULT: No bookings found for " + action.email() + ".";
                    } else {
                        String bookingSummary = bookings.stream()
                                .map(booking -> "Flight ID " + booking.id() + " (" + booking.flightNumber() + ") to " + booking.destination() + " departing at " + booking.departureTime())
                                .reduce((a, b) -> a + "; " + b)
                                .orElse("");

                        resultData = "RESULT: Found " + bookings.size() + " bookings for " + action.email() + ". " + bookingSummary;
                    }

                    List<ChatMessage> responseMessages = List.of(
                            new ChatMessage("system", "You are a helpful flight assistant. Explain results clearly to the user."),
                            new ChatMessage("user", resultData)
                    );

                    reply = openAiService.getChatResponse(responseMessages);
                } catch (Exception ex) {
                    List<ChatMessage> responseMessages = List.of(
                            new ChatMessage("system", "You are a helpful flight assistant. Explain errors clearly to the user."),
                            new ChatMessage("user", "ERROR: " + ex.getMessage())
                    );

                    reply = openAiService.getChatResponse(responseMessages);
                }
            }

            default -> reply = "I didn’t understand the request.";
        }

        memoryService.addMessage(request.sessionId(), new ChatMessage("assistant", reply));

        return ResponseEntity.ok(new AiChatResponse(reply));
    }
}