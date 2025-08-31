package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
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
public class DefendantController {
  private ChatCompletionRequest chatCompletionRequest;

  // slideshow variables
  private List<Image> images = new ArrayList<>();
  private int currentImageIndex = 0;
  private Timeline animationTime;
  private final int slideshowDuration = 2;

  @FXML private TextArea txtaChat;
  @FXML private TextField txtInput;
  @FXML private Button btnSend;
  @FXML private ImageView flashbackSlideshow;
  @FXML private javafx.scene.control.ProgressIndicator loading;

  /**
   * Initializes the chat view.
   *
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  @FXML
  public void initialize() throws ApiProxyException {
    // Any required initialization code can be placed here
    loadImages(null);
    initChat();
    loading.setProgress(-1);
  }

  // loading images for flashback
  private void loadImages(Runnable onLoaded) {
    new Thread(
            () -> {
              // loading images for animation flashback
              List<Image> loadedImages = new ArrayList<>();
              loadedImages.add(
                  new Image(
                      getClass().getResourceAsStream("/images/defendantFlashback/defFlash1.jpg")));
              loadedImages.add(
                  new Image(
                      getClass().getResourceAsStream("/images/defendantFlashback/defFlash2.jpg")));
              loadedImages.add(
                  new Image(
                      getClass().getResourceAsStream("/images/defendantFlashback/defFlash3.jpg")));
              loadedImages.add(
                  new Image(
                      getClass().getResourceAsStream("/images/defendantFlashback/defFlash4.jpg")));
              loadedImages.add(
                  new Image(
                      getClass().getResourceAsStream("/images/defendantFlashback/defFlash5.jpg")));
              Platform.runLater(
                  () -> {
                    // add all the images for viewing
                    images.clear();
                    images.addAll(loadedImages);
                    if (onLoaded != null) {
                      onLoaded.run();
                    }
                  });
            })
        .start();
  }

  // run flashback slideshow
  public void startFlashbackSlideshow() {
    // Load images and start slideshow after loading
    if (images.isEmpty()) {
      loadImages(this::startFlashbackSlideshow);
      return;
    }

    currentImageIndex = 0;
    flashbackSlideshow.setImage(images.get(currentImageIndex));
    animationTime =
        new Timeline(
            new KeyFrame(
                Duration.seconds(slideshowDuration),
                (ActionEvent event) -> {
                  currentImageIndex = (currentImageIndex + 1);
                  flashbackSlideshow.setImage(images.get(currentImageIndex));
                }));
    animationTime.setCycleCount(images.size());
    animationTime.setAutoReverse(false);
    animationTime.play();
  }

  public void runFlashback() {
    startFlashbackSlideshow();
  }

  /**
   * Generates the system prompt based on the profession.
   *
   * @return the system prompt string
   */
  private String getSystemPrompt() {
    return PromptEngineering.getPrompt("aegis.txt");
  }

  /**
   * Sets the profession for the chat context and initializes the ChatCompletionRequest.
   *
   * @param profession the profession to set
   */
  public void initChat() {
    // set loading visible
    loading.setVisible(true);
    txtInput.setDisable(true);
    btnSend.setDisable(true);

    // run chat
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

              // set loading invisible
              Platform.runLater(
                  () -> {
                    loading.setVisible(false);
                    txtInput.setDisable(false);
                    btnSend.setDisable(false);
                  });
            })
        .start();
  }

  // Syncing chat history
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
            ? "aegis"
            : msg.getRole().equals("user") ? "user" : msg.getRole();
    ChatHistory.addMessage(msg, who);

    // Display only the original message content (without speaker prefix)
    String displayRole;
    if (msg.getRole().equals("assistant")) {
      displayRole = "Aegis I";
    } else if (msg.getRole().equals("user")) {
      displayRole = "You";
    } else {
      displayRole = msg.getRole();
    }
    txtaChat.appendText(
        displayRole
            + ": "
            + msg.getContent().replaceFirst("^Aegis I said: ", "").replaceFirst("^aegis said: ", "")
            + "\n\n");
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

  // Not first time
  public void runAfterFirst() {
    flashbackSlideshow.setImage(
        new Image(getClass().getResourceAsStream("/images/postFlashback/aegis.jpeg")));
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

    // set loading symbols visible
    txtInput.clear();
    loading.setVisible(true);
    txtInput.setDisable(true);
    btnSend.setDisable(true);
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
