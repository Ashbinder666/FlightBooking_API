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
                .model(ChatModel.GPT_4_1_MINI);

        for (ChatMessage msg : messages) {
            if ("user".equals(msg.role())) {
                builder.addUserMessage(msg.content());
            } else if ("assistant".equals(msg.role())) {
                builder.addAssistantMessage(msg.content());
            } else if ("system".equals(msg.role())) {
                builder.addSystemMessage(msg.content());
            }
        }

        ChatCompletion completion = client.chat().completions().create(builder.build());

        return completion.choices()
                .get(0)
                .message()
                .content()
                .orElse("No response from model.");
    }
}