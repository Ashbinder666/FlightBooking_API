package se.lexicon.flightbooking_api.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OpenAiService {

    private final OpenAIClient client = OpenAIOkHttpClient.fromEnv();

    public String getChatResponse(List<ChatMessage> messages) {

        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_4_1_MINI)
                .addSystemMessage("""
                        You are a flight booking assistant.
                        
                        You operate in TWO MODES:
                        
                        MODE 1: ACTION MODE
                        - When the user asks to do something (search, book, cancel)
                        - Respond ONLY in JSON
                        - Use this format:
                        {
                          "action": "...",
                          "flightId": null,
                          "destination": null,
                          "name": null,
                          "email": null
                        }
                        
                        MODE 2: RESPONSE MODE
                        - When you receive a message starting with:
                          "Result:", "BOOKED:", "CANCELLED:", or "ERROR:"
                        - You MUST respond as a human assistant
                        - DO NOT return JSON
                        - Explain clearly what happened
                        
                        Rules:
                        - Never mix JSON and text
                        - Detect mode based on the latest message
                        """);

        for (ChatMessage msg : messages) {
            if ("user".equals(msg.role())) {
                builder.addUserMessage(msg.content());
            } else if ("assistant".equals(msg.role())) {
                builder.addAssistantMessage(msg.content());
            }
        }

        ChatCompletion completion = client.chat().completions().create(builder.build());

        String result = completion.choices()
                .get(0)
                .message()
                .content()
                .orElse("{}");

        System.out.println("AI RAW RESPONSE: " + result);

        return result;
    }
}