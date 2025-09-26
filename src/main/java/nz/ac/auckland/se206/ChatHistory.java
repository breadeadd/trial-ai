package nz.ac.auckland.se206;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;

public class ChatHistory {
  private static final List<ChatMessage> history = new ArrayList<>();
  private static final Map<String, List<ChatMessage>> characterContexts = new HashMap<>();

  public static void addMessage(ChatMessage msg, String who) {
    // Determine speaker prefix for context tracking
    String prefix = "";
    switch (who) {
      case "aegis":
      case "Aegis I":
        prefix = "Aegis I said: ";
        break;
      case "orion":
      case "Orion Vale":
        prefix = "Orion Vale said: ";
        break;
      case "echo":
      case "Echo II":
        prefix = "Echo II said: ";
        break;
      case "user":
      case "User":
        prefix = "User said: ";
        break;
      default:
        prefix = who + " said: ";
        break;
    }
    // Create message with speaker context and add to shared history
    ChatMessage contextualMsg = new ChatMessage(msg.getRole(), prefix + msg.getContent());
    history.add(contextualMsg);
  }

  public static void addCharacterContext(ChatMessage msg, String characterName) {
    // Store character-specific conversation context
    characterContexts.computeIfAbsent(characterName, k -> new ArrayList<>()).add(msg);
  }

  public static List<ChatMessage> getHistory() {
    return Collections.unmodifiableList(history);
  }

  public static List<ChatMessage> getHistoryWithCharacterContext(String characterName) {
    List<ChatMessage> combined = new ArrayList<>(history);
    List<ChatMessage> characterContext = characterContexts.get(characterName);
    if (characterContext != null) {
      combined.addAll(characterContext);
    }
    return combined;
  }
}
