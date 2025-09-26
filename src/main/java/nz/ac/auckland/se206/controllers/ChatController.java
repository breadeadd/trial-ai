package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.animation.TranslateTransition;
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
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
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
    // Move to the next image in the sequence
    currentIndex++;
    
    // Check if there are more images to display
    if (currentIndex < images.size()) {
      // Display the next image in the slideshow
      flashbackImageView.setImage(images.get(currentIndex));
    } else {
      // End of slideshow reached - disable further mouse interactions
      flashbackImageView.setOnMouseClicked(null);
    }
    
    // Return updated index for caller to track current position
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
    // Show the popup overlay to indicate interactive mode
    popupPane.setVisible(true);
    
    // Hide the next button since user can now interact with memory elements
    nextButton.setVisible(false);
    
    // Enable the back button for navigation
    backBtn.setDisable(false);

    // Make all chat interface elements visible for user interaction
    setChatUiVisibility(true);
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

      setChatUiVisibility(false);
      return false;
    } else {
      // Drop up (show)
      animateTranslateMethod.accept(txtaChat, 0.0);
      animateTranslateMethod.accept(txtInput, 0.0);
      animateTranslateMethod.accept(btnSend, 0.0);

      setChatUiVisibility(true);
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

  /**
   * Common utility method for loading and setting arrow images on buttons.
   * This method is used across multiple controllers to maintain consistent arrow styling.
   *
   * @param button the button to set the image on
   * @param imagePath the path to the image resource
   */
  protected void setArrowImage(Button button, String imagePath) {
    try {
      // Load image from resources and configure size
      Image arrowImage = new Image(getClass().getResourceAsStream(imagePath));
      ImageView imageView = new ImageView(arrowImage);
      imageView.setFitWidth(40);
      imageView.setFitHeight(40);
      imageView.setPreserveRatio(true);
      button.setGraphic(imageView);
      button.setText("");
      button.setStyle("-fx-background-color: transparent;");
    } catch (Exception e) {
      System.err.println("Could not load arrow image: " + imagePath);
      // Fallback to text if image fails
      button.setGraphic(null);
      button.setText("▼");
    }
  }

  /**
   * Common utility method for updating arrow button to drop-down state.
   * Positions the arrow above the chat area and sets the appropriate image.
   *
   * @param dropUpArrow the button to configure
   */
  protected void updateArrowToDropDown(Button dropUpArrow) {
    dropUpArrow.setLayoutX(14.0);
    dropUpArrow.setLayoutY(400.0);
    setArrowImage(dropUpArrow, "/images/assets/chatDown.png");
  }

  /**
   * Common utility method for updating arrow button to drop-up state.
   * Positions the arrow below the chat area and sets the appropriate image.
   *
   * @param dropUpArrow the button to configure
   */
  protected void updateArrowToDropUp(Button dropUpArrow) {
    dropUpArrow.setLayoutX(14.0);
    dropUpArrow.setLayoutY(540.0);
    setArrowImage(dropUpArrow, "/images/assets/chatUp.png");
  }

  /**
   * Creates smooth vertical slide animation for UI elements during chat toggle operations.
   * This method provides a consistent animation experience when showing or hiding chat
   * interface components, enhancing the user experience with fluid visual transitions.
   *
   * @param node the UI node to animate (typically chat area, input field, or send button)
   * @param toY the target Y position for the animation (vertical offset)
   */
  protected void animateTranslate(Node node, double toY) {
    // Configure and start translation animation with 300ms duration
    TranslateTransition transition = new TranslateTransition(Duration.millis(300), node);
    transition.setToY(toY);
    transition.play();
  }

  /**
   * Loads a list of images from specified paths in a background thread.
   * This method provides consistent image loading behavior across all chat controllers.
   *
   * @param imagePaths array of image resource paths to load
   * @param onLoaded callback to execute when all images are loaded
   * @return list of loaded Image objects
   */
  protected List<Image> loadImagesInBackground(String[] imagePaths, Runnable onLoaded) {
    List<Image> images = new ArrayList<>();
    // Load images in background thread to avoid blocking UI
    new Thread(() -> {
      try {
        for (String path : imagePaths) {
          Image image = new Image(getClass().getResourceAsStream(path));
          images.add(image);
        }
        // Execute callback on JavaFX thread when loading complete
        Platform.runLater(onLoaded);
      } catch (Exception e) {
        System.err.println("Error loading images: " + e.getMessage());
        Platform.runLater(onLoaded); // Still execute callback even if loading fails
      }
    }).start();
    return images;
  }

  /**
   * Clears the chat UI text area, removing all displayed messages.
   * This method provides a standard way to reset chat interfaces.
   */
  public void clearChatUi() {
    if (txtaChat != null) {
      txtaChat.clear(); // Remove all text from chat display area
    }
  }

  /**
   * Resets an ImageView to its default visual state (opacity 1.0, scale 1.0, no translation).
   * Common utility for resetting UI elements to their original appearance.
   * 
   * @param imageView the ImageView to reset, can be null (will be ignored)
   */
  protected void resetImageProperties(ImageView imageView) {
    if (imageView != null) {
      imageView.setOpacity(1.0);
      imageView.setScaleX(1.0);
      imageView.setScaleY(1.0);
      imageView.setTranslateX(0);
      imageView.setTranslateY(0);
    }
  }

  /**
   * Sets the visibility of the main chat UI elements (chat area, input field, send button).
   * This is a common operation across controllers to show/hide chat interface.
   * 
   * @param visible true to show the chat UI elements, false to hide them
   */
  protected void setChatUiVisibility(boolean visible) {
    if (txtaChat != null) {
      txtaChat.setVisible(visible);
    }
    if (txtInput != null) {
      txtInput.setVisible(visible);
    }
    if (btnSend != null) {
      btnSend.setVisible(visible);
    }
  }

  /**
   * Executes a task after a specified delay on a background thread.
   * This is a common pattern for timed message delivery and UI updates.
   * 
   * @param delayMs delay in milliseconds before executing the task
   * @param task the task to execute after the delay
   */
  protected void executeDelayedTask(long delayMs, Runnable task) {
    // Create a new thread for the delayed execution
    Thread delayedThread = new Thread(() -> {
      // Sleep for the specified delay
      try {
        Thread.sleep(delayMs);
        Platform.runLater(task);
      } catch (InterruptedException e) {
        // Handle interruption
        Thread.currentThread().interrupt();
      }
    });
    // Mark the thread as a daemon thread
    delayedThread.setDaemon(true);
    delayedThread.start();
  }

  /**
   * Sends a message to chat after a specified delay.
   * Common pattern across controllers for timed message delivery.
   * 
   * @param delayMs delay in milliseconds before sending the message
   * @param message the message content to send
   * @param role the message role (usually "assistant")
   */
  protected void sendDelayedMessage(long delayMs, String message, String role) {
    executeDelayedTask(delayMs, () -> {
      ChatMessage delayedMessage = new ChatMessage(role, message);
      // Add to chat history if this is from a character
      if ("assistant".equals(role)) {
        ChatHistory.addMessage(delayedMessage, getCharacterName());
      }
      appendChatMessage(delayedMessage);
    });
  }

  /**
   * Sets up and configures a MediaPlayer for audio playback with standard error handling.
   * This method creates a consistent audio setup pattern used across multiple controllers
   * for playing TTS audio and other sound effects during game interactions.
   * 
   * @param audioResourcePath the path to the audio resource (e.g., "/audio/openTts.mp3")
   * @param volume the playback volume (0.0 to 1.0)
   * @return configured MediaPlayer instance ready for playback
   * @throws Exception if there is an error loading or configuring the audio
   */
  protected MediaPlayer setupMediaPlayer(String audioResourcePath, double volume) throws Exception {
    // Load and configure audio file using resource URI
    String audioPath = getClass().getResource(audioResourcePath).toURI().toString();
    Media media = new Media(audioPath);
    MediaPlayer player = new MediaPlayer(media);
    
    // Configure playback settings
    player.setVolume(volume);
    
    // Auto-play when audio file is ready for playback
    player.setOnReady(() -> player.play());
    
    // Handle any playback errors that occur during audio processing
    player.setOnError(() -> {
      if (player.getError() != null) {
        player.getError().printStackTrace();
      }
    });
    
    return player;
  }

  /**
   * Initializes flashback slideshow with consistent loading and display behavior.
   * This method provides a standard approach for setting up image slideshows
   * across different character controllers, ensuring uniform user experience.
   * 
   * @param images the list of images to display in the slideshow
   * @param flashbackImageView the ImageView component to display slideshow images
   * @param currentIndex reference to current image index (will be set to 0)
   * @param onLoadComplete callback to execute after images are loaded
   */
  protected void initializeFlashbackSlideshow(List<Image> images, ImageView flashbackImageView, 
      Runnable onLoadComplete) {
    // Load images if not already loaded, then initialize slideshow
    if (images.isEmpty() && onLoadComplete != null) {
      onLoadComplete.run(); // This should trigger image loading
      return;
    }
    
    // Set slideshow to first image and make it visible
    if (!images.isEmpty() && flashbackImageView != null) {
      flashbackImageView.setImage(images.get(0));
      flashbackImageView.setVisible(true);
    }
  }

  /**
   * Resets chat UI elements to their initial state with proper positioning.
   * This method provides consistent reset behavior across controllers when
   * restarting the game or returning to initial states.
   * 
   * @param chatVisible the initial chat visibility state to set
   */
  protected void resetChatUiElements(boolean chatVisible) {
    Platform.runLater(() -> {
      // Reset chat interface elements to initial positions and visibility
      if (txtaChat != null) {
        txtaChat.setTranslateY(0);
        txtaChat.setVisible(chatVisible);
      }
      if (txtInput != null) {
        txtInput.setTranslateY(0);
        txtInput.setVisible(chatVisible);
      }
      if (btnSend != null) {
        btnSend.setTranslateY(0);
        btnSend.setVisible(chatVisible);
      }
    });
  }
}
