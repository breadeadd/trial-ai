package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.List;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import java.util.function.BiConsumer;
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
  @FXML protected ProgressIndicator loading;

  // Methods to get specific details:
  protected abstract String getSystemPrompt();

  // speaker context in chat history
  protected abstract String getCharacterName();

  // for UI display purposes
  protected abstract String getDisplayRole();

  /** Initializes the ChatCompletionRequest and starts the chat. */
  public void initChat() {
    // Initialize chat in background thread to avoid UI blocking
    new Thread(
            () -> {
              try {
                // Configure GPT chat parameters
                ApiProxyConfig config = ApiProxyConfig.readConfig();
                chatCompletionRequest =
                    new ChatCompletionRequest(config)
                        .setN(1)
                        .setTemperature(0.2)
                        .setTopP(0.5)
                        .setModel(Model.GPT_4_1_MINI)
                        .setMaxTokens(100);
                // Send initial system prompt to establish character context
                runGpt(new ChatMessage("system", getSystemPrompt()));
              } catch (ApiProxyException e) {
                e.printStackTrace();
              }
            })
        .start();
  }

  /** Syncs chat history for this character. */
  public void syncChatHistoryAsync() {
    // Update character's context with shared conversation history
    new Thread(
            () -> {
              try {
                // Create new request with updated history
                ApiProxyConfig config = ApiProxyConfig.readConfig();
                ChatCompletionRequest newRequest =
                    new ChatCompletionRequest(config)
                        .setN(1)
                        .setTemperature(0.2)
                        .setTopP(0.5)
                        .setModel(Model.GPT_4_1_MINI)
                        .setMaxTokens(100);
                // Add system prompt and conversation history
                newRequest.addMessage(new ChatMessage("system", getSystemPrompt()));
                for (ChatMessage msg :
                    ChatHistory.getHistoryWithCharacterContext(getCharacterName())) {
                  newRequest.addMessage(msg);
                }
                // Update request on UI thread
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

    // Display the message
    displayChatMessage(msg);
  }

  /**
   * Displays a chat message in the text area without adding to ChatHistory. Used when the message
   * has already been added to ChatHistory separately.
   *
   * @param msg the chat message to display
   */
  protected void displayChatMessage(ChatMessage msg) {
    // Display the original message content without any modification
    String displayRole;
    if (msg.getRole().equals("assistant")) {
      displayRole = getDisplayRole();
    } else if (msg.getRole().equals("user")) {
      displayRole = "You";
    } else {
      displayRole = msg.getRole();
    }

    if (txtaChat != null) {
      txtaChat.appendText(displayRole + ": " + msg.getContent() + "\n\n");
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

      // Clean the AI's response by removing character name prefix if present
      String cleanedContent = responseMsg.getContent();
      String characterName = getDisplayRole();

      // Try multiple prefix patterns that the AI might use
      String[] possiblePrefixes = {
        characterName + " said: ",
        characterName + ": ",
        characterName + " said:",
        characterName + ":"
      };

      for (String prefix : possiblePrefixes) {
        if (cleanedContent.startsWith(prefix)) {
          cleanedContent = cleanedContent.substring(prefix.length());
          break;
        }
      }

      // Create a new message with cleaned content for display
      ChatMessage cleanedResponse = new ChatMessage(responseMsg.getRole(), cleanedContent);

      // Add the original response (with prefix) to the request for AI context
      chatCompletionRequest.addMessage(responseMsg);

      // Add the original response to ChatHistory for context (this will have "Character said:"
      // prefix)
      String speaker = responseMsg.getRole().equals("assistant") ? getCharacterName() : "User";
      ChatHistory.addMessage(responseMsg, speaker);

      // Display only the cleaned response (without calling appendChatMessage to avoid double
      // ChatHistory entry)
      displayChatMessage(cleanedResponse);

      return cleanedResponse;
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
  // Handles Enter key press to send chat messages
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
   * Common utility method for advancing flashback slideshow to next image.
   *
   * @param currentIndex current image index
   * @param images list of images
   * @param flashbackImageView the ImageView displaying the slideshow
   * @return new current image index
   */
  protected int advanceFlashbackSlideshow(
      int currentIndex, 
      List<Image> images, 
      ImageView flashbackImageView) {
    currentIndex++;
    if (currentIndex < images.size()) {
      flashbackImageView.setImage(images.get(currentIndex));
    } else {
      flashbackImageView.setOnMouseClicked(null);
    }
    return currentIndex;
  }

  /**
   * Common utility method for showing memory screen user interface elements.
   *
   * @param popupPane the popup pane to show
   * @param nextButton the next button to hide
   * @param backBtn the back button to enable
   */
  protected void showMemoryScreenUserInterface(
      Pane popupPane, 
      Button nextButton, 
      Button backBtn) {
    popupPane.setVisible(true);
    nextButton.setVisible(false);
    backBtn.setDisable(false);

    btnSend.setVisible(true);
    txtInput.setVisible(true);
    txtaChat.setVisible(true);
  }

  /**
   * Common utility method for toggling chat visibility with animations.
   * Subclasses should call this method and handle arrow updates separately.
   *
   * @param chatVisible current chat visibility state
   * @param animateTranslateMethod method reference for animation
   * @return new chat visibility state
   */
  protected boolean toggleChatVisibility(
      boolean chatVisible, 
      BiConsumer<Node, Double> animateTranslateMethod) {
    if (chatVisible) {
      // Drop down (hide)
      animateTranslateMethod.accept(txtaChat, 150.0);
      animateTranslateMethod.accept(txtInput, 150.0);
      animateTranslateMethod.accept(btnSend, 150.0);

      btnSend.setVisible(false);
      txtInput.setVisible(false);
      txtaChat.setVisible(false);
      return false;
    } else {
      // Drop up (show)
      animateTranslateMethod.accept(txtaChat, 0.0);
      animateTranslateMethod.accept(txtInput, 0.0);
      animateTranslateMethod.accept(btnSend, 0.0);

      btnSend.setVisible(true);
      txtInput.setVisible(true);
      txtaChat.setVisible(true);
      return true;
    }
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
    App.setRoot("room");

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
