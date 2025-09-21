package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest.Model;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.ChatHistory;
import nz.ac.auckland.se206.states.GameStateManager;

/**
 * Controller class for the chat view. Handles user interactions and communication with the GPT
 * model via the API proxy.
 */
public abstract class ChatController {
  protected ChatCompletionRequest chatCompletionRequest;

  @FXML protected TextArea txtaChat;
  @FXML protected TextField txtInput;
  @FXML protected Button btnSend;
  @FXML protected javafx.scene.control.ProgressIndicator loading;

  // Methods to get specific details:
  protected abstract String getSystemPrompt();

  // speaker context in chat history
  protected abstract String getCharacterName();

  // for UI display purposes
  protected abstract String getDisplayRole();

  /** Initializes the ChatCompletionRequest and starts the chat. */
  public void initChat() {

    new Thread(
            () -> {
              try {
                ApiProxyConfig config = ApiProxyConfig.readConfig();
                chatCompletionRequest =
                    new ChatCompletionRequest(config)
                        .setN(1)
                        .setTemperature(0.2)
                        .setTopP(0.5)
                        .setModel(Model.GPT_4_1_MINI)
                        .setMaxTokens(100);
                runGpt(new ChatMessage("system", getSystemPrompt()));
              } catch (ApiProxyException e) {
                e.printStackTrace();
              }
            })
        .start();
  }

  /** Syncs chat history for this character. */
  public void syncChatHistoryAsync() {
    new Thread(
            () -> {
              try {
                ApiProxyConfig config = ApiProxyConfig.readConfig();
                ChatCompletionRequest newRequest =
                    new ChatCompletionRequest(config)
                        .setN(1)
                        .setTemperature(0.2)
                        .setTopP(0.5)
                        .setModel(Model.GPT_4_1_MINI)
                        .setMaxTokens(100);
                newRequest.addMessage(new ChatMessage("system", getSystemPrompt()));
                for (ChatMessage msg : ChatHistory.getHistory()) {
                  newRequest.addMessage(msg);
                }
                Platform.runLater(() -> chatCompletionRequest = newRequest);
              } catch (Exception e) {
                e.printStackTrace();
              }
            })
        .start();
  }

  /**
   * Appends a chat message to the chat text area.
   *
   * @param msg the chat message to append
   */
  protected void appendChatMessage(ChatMessage msg) {
    // Store message in shared chat history with speaker context
    String speaker = msg.getRole().equals("assistant") ? getCharacterName() : "User";
    ChatHistory.addMessage(msg, speaker);

    // Display only the original message content (without speaker prefix)
    String displayRole;
    if (msg.getRole().equals("assistant")) {
      displayRole = getDisplayRole();
    } else if (msg.getRole().equals("user")) {
      displayRole = "You";
    } else {
      displayRole = msg.getRole();
    }

    if (txtaChat != null) {
      txtaChat.appendText(
          displayRole
              + ": "
              + msg.getContent()
                  .replaceFirst("^User said: ", "")
                  .replaceFirst("^Aegis I said: ", "")
                  .replaceFirst("^Echo II said: ", "")
                  .replaceFirst("^Orion Vale said: ", "")
              + "\n\n");
    }
  }

  /**
   * Runs the GPT model with a given chat message.
   *
   * @param msg the chat message to process
   * @return the response chat message
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  protected ChatMessage runGpt(ChatMessage msg) throws ApiProxyException {
    chatCompletionRequest.addMessage(msg);
    try {
      ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();
      Choice result = chatCompletionResult.getChoices().iterator().next();
      ChatMessage responseMsg = result.getChatMessage();
      // Don't prepend character name here - ChatHistory will handle speaker context
      chatCompletionRequest.addMessage(responseMsg);
      appendChatMessage(responseMsg);
      return responseMsg;
    } catch (ApiProxyException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Sends a message to the GPT model.
   *
   * @param event the action event triggered by the send button
   * @throws ApiProxyException if there is an error communicating with the API proxy
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void onKeyPressed(KeyEvent event) throws ApiProxyException, IOException {
    if (event.getCode() == KeyCode.ENTER) {
      onSendMessage(null); // Call existing send method
    }
  }

  @FXML
  protected void onSendMessage(ActionEvent event) throws ApiProxyException, IOException {
    String message = txtInput.getText().trim();
    if (message.isEmpty()) {
      return;
    }
    txtInput.clear();
    if (loading != null) {
      loading.setVisible(true);
    }
    txtInput.setDisable(true);
    btnSend.setDisable(true);

    ChatMessage msg = new ChatMessage("user", message);
    appendChatMessage(msg);

    // Mark character as talked to
    GameStateManager.getInstance().setCharacterTalkedTo(getCharacterName());

    new Thread(
            () -> {
              try {
                runGpt(msg);
              } catch (ApiProxyException e) {
                e.printStackTrace();
              }
              Platform.runLater(
                  () -> {
                    // set loading symbols invisible
                    if (loading != null) {
                      loading.setVisible(false);
                    }
                    txtInput.setDisable(false);
                    btnSend.setDisable(false);
                  });
            })
        .start();
  }

  /**
   * Navigates back to the previous view.
   *
   * @param event the action event triggered by the go back button
   * @throws ApiProxyException if there is an error communicating with the API proxy
   * @throws IOException if there is an I/O error
   */
  @FXML
  protected void onGoBack(ActionEvent event) throws ApiProxyException, IOException {
    nz.ac.auckland.se206.App.setRoot("room");

    // update button state on back
    Platform.runLater(
        () -> {
          RoomController roomController = (RoomController) App.getController("room");
          if (roomController != null) {
            roomController.updateButtonState();
          }
        });
  }
}
