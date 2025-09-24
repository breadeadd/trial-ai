package nz.ac.auckland.se206;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;

public class ChatHistory {
  private static final List<ChatMessage> history = new ArrayList<>();

  public static void addMessage(ChatMessage msg, String who) {
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
    ChatMessage contextualMsg = new ChatMessage(msg.getRole(), prefix + msg.getContent());
    history.add(contextualMsg);
  }

  public static List<ChatMessage> getHistory() {
    return Collections.unmodifiableList(history);
  }
}
