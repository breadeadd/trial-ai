package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest.Model;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.ChatHistory;
import nz.ac.auckland.se206.prompts.PromptEngineering;

/**
 * Controller class for the chat view. Handles user interactions and communication with the GPT
 * model via the API proxy.
 */
public class AiWitnessController {
  private ChatCompletionRequest chatCompletionRequest;

  @FXML private TextArea txtaChat;
  @FXML private TextField txtInput;
  @FXML private Button btnSend;
  @FXML private ImageView aiFlashback;
  @FXML private javafx.scene.control.ProgressIndicator loading;

  /**
   * Initializes the chat view.
   *
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  @FXML
  public void initialize() throws ApiProxyException {
    // Any required initialization code can be placed here
    loading.setProgress(-1);
    initChat();
  }

  /**
   * Generates the system prompt based on the profession.
   *
   * @return the system prompt string
   */
  private String getSystemPrompt() {
    return PromptEngineering.getPrompt("echo.txt");
  }

  /**
   * Sets the profession for the chat context and initializes the ChatCompletionRequest.
   *
   * @param profession the profession to set
   */
  public void initChat() {
    // set loading symbols visible
    loading.setVisible(true);
    txtInput.setDisable(true);
    btnSend.setDisable(true);

    new Thread(
            () -> {
              try {
                // initializing the chatbot
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
              Platform.runLater(
                  () -> {
                    // when loaded - remove loading and let user interact
                    loading.setVisible(false);
                    txtInput.setDisable(false);
                    btnSend.setDisable(false);
                  });
            })
        .start();
  }

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
                // Always add the system prompt first
                newRequest.addMessage(new ChatMessage("system", getSystemPrompt()));
                for (ChatMessage msg : ChatHistory.getHistory()) {
                  newRequest.addMessage(msg);
                }
                // Update the field on the JavaFX thread
                Platform.runLater(() -> chatCompletionRequest = newRequest);
              } catch (ApiProxyException e) {
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
  private void appendChatMessage(ChatMessage msg) {
    // Store message in shared chat history with speaker context
    String who =
        msg.getRole().equals("assistant")
            ? "echo"
            : msg.getRole().equals("user") ? "user" : msg.getRole();
    ChatHistory.addMessage(msg, who);

    // Display only the original message content (without speaker prefix)
    String displayRole;
    if (msg.getRole().equals("assistant")) {
      displayRole = "Echo II";
    } else if (msg.getRole().equals("user")) {
      displayRole = "You";
    } else {
      displayRole = msg.getRole();
    }
    txtaChat.appendText(
        displayRole
            + ": "
            + msg.getContent().replaceFirst("^Echo II said: ", "").replaceFirst("^echo said: ", "")
            + "\n\n");
  }

  // Not first time visiting
  public void runAfterFirst() {
    aiFlashback.setImage(
        new Image(getClass().getResourceAsStream("/images/postFlashback/echo.jpeg")));
  }

  /**
   * Runs the GPT model with a given chat message.
   *
   * @param msg the chat message to process
   * @return the response chat message
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  private ChatMessage runGpt(ChatMessage msg) throws ApiProxyException {
    chatCompletionRequest.addMessage(msg);
    try {
      ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();
      Choice result = chatCompletionResult.getChoices().iterator().next();
      chatCompletionRequest.addMessage(result.getChatMessage());
      appendChatMessage(result.getChatMessage());
      return result.getChatMessage();
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
  private void onSendMessage(ActionEvent event) throws ApiProxyException, IOException {
    String message = txtInput.getText().trim();
    if (message.isEmpty()) {
      return;
    }
    txtInput.clear();

    // set loading symbols visible
    loading.setVisible(true);
    txtInput.setDisable(true);
    btnSend.setDisable(true);
    // Store message in chat history
    ChatMessage msg = new ChatMessage("user", message);
    appendChatMessage(msg);
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
                    loading.setVisible(false);
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
  private void onGoBack(ActionEvent event) throws ApiProxyException, IOException {
    nz.ac.auckland.se206.App.setRoot("room");
  }
}
