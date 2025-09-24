package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.ChatHistory;
import nz.ac.auckland.se206.prompts.PromptEngineering;
import nz.ac.auckland.se206.states.GameStateManager;

/**
 * Controller class for the chat view. Handles user interactions and communication with the GPT
 * model via the API proxy.
 */
public class HumanWitnessController extends ChatController {
  private List<Image> images = new ArrayList<>();
  private int currentImageIndex = 0;
  private boolean chatVisible = true; // Track chat visibility state, default to visible

  @FXML private ImageView flashbackSlideshow;
  @FXML private Rectangle screenBox;
  @FXML private Button nextButton;
  @FXML private Slider unlockSlider;
  @FXML private Button dropUpArrow;
  @FXML private TextArea txtaChat;
  @FXML private TextField txtInput;
  @FXML private Button btnSend;
  @FXML private Button backBtn;

  /**
   * Initializes the chat view.
   *
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  @FXML
  public void initialize() throws ApiProxyException {
    loadImages(null);
    initChat();

    btnSend.setVisible(false);
    txtInput.setVisible(false);
    txtaChat.setVisible(false);
    backBtn.setDisable(true);

    unlockSlider.setVisible(false); // Slider is initially hidden
    dropUpArrow.setVisible(false); // Drop up arrow initially hidden
  }

  @Override
  protected String getSystemPrompt() {
    return PromptEngineering.getPrompt("orion.txt");
  }

  @Override
  protected String getCharacterName() {
    return "Orion Vale";
  }

  @Override
  protected String getDisplayRole() {
    return "Orion Vale";
  }

  // Run flashback slideshow
  public void startFlashbackSlideshow() {
    if (images.isEmpty()) {
      loadImages(this::startFlashbackSlideshow);
      return;
    }
    currentImageIndex = 0;
    flashbackSlideshow.setImage(images.get(currentImageIndex));
  }

  public void runFlashback() {
    startFlashbackSlideshow();
  }

  // Not first time
  public void runAfterFirst() {
    if (GameStateManager.getInstance().getInteractionFlag("OrionInt") == true) {
      currentImageIndex = 4; // Start at humanMem2.png
      flashbackSlideshow.setImage(images.get(currentImageIndex));
    } else {
      currentImageIndex = 3; // Start at humanMem1.png
      flashbackSlideshow.setImage(images.get(currentImageIndex));
      unlockSlider.setVisible(true); // Show slider when on humanMem1.png
      unlockSlider.setDisable(false);
      unlockSlider.setValue(0.0); // Resetting to starting pos
      dropUpArrow.setVisible(true); // Show arrow on humanMem1.png
      // Set to dropUpArrow shape
      updateArrowToDropDown();
    }
  }

  // Loading images for flashback
  private void loadImages(Runnable onLoaded) {
    new Thread(
            () -> {
              List<Image> loadedImages = new ArrayList<>();
              loadedImages.add(
                  new Image(
                      getClass().getResourceAsStream("/images/flashbacks/human/human1F.png")));
              loadedImages.add(
                  new Image(
                      getClass().getResourceAsStream("/images/flashbacks/human/human2F.png")));
              loadedImages.add(
                  new Image(
                      getClass().getResourceAsStream("/images/flashbacks/human/human3F.png")));
              loadedImages.add(
                  new Image(getClass().getResourceAsStream("/images/memories/humanMem1.png")));
              loadedImages.add(
                  new Image(getClass().getResourceAsStream("/images/memories/humanMem2.png")));
              Platform.runLater(
                  () -> {
                    images.clear();
                    images.addAll(loadedImages);
                    if (onLoaded != null) {
                      onLoaded.run();
                    }
                  });
            })
        .start();
  }

  // Change to next scene
  @FXML
  protected void nextScene(ActionEvent event) throws ApiProxyException, IOException {
    currentImageIndex++;
    if (currentImageIndex < images.size()) {
      flashbackSlideshow.setImage(images.get(currentImageIndex));
    } else {
      flashbackSlideshow.setOnMouseClicked(null);
    }

    // flashback ends and chat begins
    if (currentImageIndex == 3) {
      nextButton.setVisible(false);
      backBtn.setDisable(false);

      unlockSlider.setVisible(true);
      unlockSlider.setDisable(false);
      dropUpArrow.setVisible(true);
      updateArrowToDropDown(); // Arrow starts pointing down

      // Initialize chat as visible
      chatVisible = true;
      txtaChat.setTranslateY(0.0);
      txtInput.setTranslateY(0.0);
      btnSend.setTranslateY(0.0);
      txtaChat.setVisible(true); // Ensure chat, text field, and send button is visible
      txtInput.setVisible(true);
      btnSend.setVisible(true);
    } else if (currentImageIndex == 4) { // When reaching humanMem2.png
      unlockSlider.setVisible(false);
      dropUpArrow.setVisible(true);

      // Chat remains in current state, arrow set based on visibility
      chatVisible = true;
      txtaChat.setTranslateY(0.0);
      txtInput.setTranslateY(0.0);
      btnSend.setTranslateY(0.0);
      if (chatVisible) {
        updateArrowToDropDown();
      } else {
        updateArrowToDropUp();
      }
      txtaChat.setVisible(true);
      txtInput.setVisible(true);
      btnSend.setVisible(true);
    } else {
      dropUpArrow.setVisible(false); // Hide drop up arrow on other screens
      unlockSlider.setVisible(false);
      btnSend.setVisible(false);
      txtInput.setVisible(false);
      txtaChat.setVisible(false);
    }
  }

  // Handle slider release to transition to humanMem2.png and hide slider
  @FXML
  protected void onSliderReleased() {
    if (currentImageIndex == 3 && unlockSlider.getValue() >= 100.0) {
      currentImageIndex = 4; // Move to humanMem2.png
      GameStateManager.getInstance().setInteractionFlag("OrionInt", true);
      flashbackSlideshow.setImage(images.get(currentImageIndex));
      unlockSlider.setDisable(true);
      unlockSlider.setVisible(false);
      dropUpArrow.setVisible(true);

      // Chat remains in current state, arrow stays based on visibility
      txtaChat.setTranslateY(chatVisible ? 0.0 : 150.0);
      txtInput.setTranslateY(chatVisible ? 0.0 : 150.0);
      btnSend.setTranslateY(chatVisible ? 0.0 : 150.0);
      if (chatVisible) {
        updateArrowToDropDown();
      } else {
        updateArrowToDropUp();
      }
      txtaChat.setVisible(chatVisible);
      txtInput.setVisible(chatVisible);
      btnSend.setVisible(chatVisible);
      
      // Add context for phone unlock event
      addContextToChat("system", "Player has successfully unlocked Orion Vale's phone through the slider mechanism. " +
          "The phone was secured but the player used the interaction slider to bypass security. " +
          "The phone contained crucial mission intelligence including details about Project Starlight compromise, " +
          "evidence of Cassian's data manipulation activities, and information about Aegis I's extreme counter-threat protocols. " +
          "This unlock reveals key evidence about the mission timeline and character motivations.");
      addContextToChat("system", "Orion Vale now has access to critical information showing: " +
          "1) Cassian's role in compromising mission data and statistics, " +
          "2) Aegis I's tendency toward immediate and extreme retaliatory actions, " +
          "3) The cascade of events that led to mission compromise and communication overflow. " +
          "This phone contains evidence that connects all three main characters to the incident.");
      

      // Send phone unlock messages with timing
      sendPhoneUnlockMessages();
    }
  }
  
  // Send phone unlock messages with timing
  private void sendPhoneUnlockMessages() {
    // Immediately show phone unlock message
    Platform.runLater(() -> {
      displayMessage("Phone Unlocked 🔓");
    });
    
    // Wait 1 second then show witness response in new thread
    Thread messageThread = new Thread(() -> {
      try {
        Thread.sleep(1000); // 1 second delay
        Platform.runLater(() -> {
          displayMessage("So Cassian compromised the mission, which makes Aegis's reaction to protect it understandable. But its methods were EXTREME. Cassian could've been in action for good.");
        });
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    messageThread.setDaemon(true);
    messageThread.start();
  }
  
  // Display a message in the chat area
  private void displayMessage(String message) {
    // Create ChatMessage and use the parent class method for consistency
    if (message.startsWith("Phone Unlocked")) {
      // System message - use user role
      ChatMessage chatMessage = new ChatMessage("user", message);
      appendChatMessage(chatMessage);
    } else {
      // Witness message - use assistant role (ChatController will handle the display name)
      ChatMessage chatMessage = new ChatMessage("assistant", message);
      appendChatMessage(chatMessage);
    }
  }
  
  // Add context to chat history without displaying to user (for AI context)
  private void addContextToChat(String role, String contextMessage) {
    ChatMessage contextChatMessage = new ChatMessage(role, contextMessage);
    ChatHistory.addMessage(contextChatMessage, "system");
    // Note: This doesn't update the UI, only the chat history for AI context
  }

  // Toggle chat visibility with drop-up/down animation
  @FXML
  private void toggleChatVisibility(ActionEvent event) {
    if (chatVisible) {
      // Drop up (hide)
      animateTranslate(txtaChat, 150.0);
      animateTranslate(txtInput, 150.0);
      animateTranslate(btnSend, 150.0);

      // Change to dropUpArrow shape and move below
      updateArrowToDropUp();
      chatVisible = false;
      btnSend.setVisible(false);
      txtInput.setVisible(false);
      txtaChat.setVisible(false);
    } else {
      // Drop down
      animateTranslate(txtaChat, 0.0);
      animateTranslate(txtInput, 0.0);
      animateTranslate(btnSend, 0.0);

      // Change to dropDownArrow shape and position above chatbox
      updateArrowToDropDown();
      chatVisible = true;
      btnSend.setVisible(true);
      txtInput.setVisible(true);
      txtaChat.setVisible(true);
    }
  }

  // arrow image
  private void setArrowImage(String imagePath) {
    try {
      Image arrowImage = new Image(getClass().getResourceAsStream(imagePath));
      ImageView imageView = new ImageView(arrowImage);
      imageView.setFitWidth(40);
      imageView.setFitHeight(40);
      imageView.setPreserveRatio(true);
      dropUpArrow.setGraphic(imageView);
      dropUpArrow.setText(""); // Remove any text
      dropUpArrow.setStyle("-fx-background-color: transparent;"); // Make background transparent
    } catch (Exception e) {
      System.err.println("Could not load arrow image: " + imagePath);
      // Fallback to text if image fails
      dropUpArrow.setGraphic(null);
      dropUpArrow.setText("▼");
    }
  }

  // Update arrow to dropDown shape and position above chatbox
  private void updateArrowToDropDown() {
    dropUpArrow.setLayoutX(14.0);
    dropUpArrow.setLayoutY(400.0);
    setArrowImage("/images/assets/chatDown.png");
  }

  // Update arrow to dropUp shape and original position
  private void updateArrowToDropUp() {
    dropUpArrow.setLayoutX(14.0);
    dropUpArrow.setLayoutY(540.0);
    setArrowImage("/images/assets/chatUp.png");
  }

  // Animate the transition
  private void animateTranslate(javafx.scene.Node node, double toY) {
    TranslateTransition transition = new TranslateTransition(Duration.millis(300), node);
    transition.setToY(toY);
    transition.play();
  }

  /** Resets the controller to its initial state for game restart. */
  public void resetControllerState() {
    Platform.runLater(
        () -> {
          // Reset image index to beginning
          currentImageIndex = 0;

          // Reset chat visibility to visible
          chatVisible = true;

          // Reset chat UI elements to start positions
          if (txtaChat != null) {
            txtaChat.setTranslateY(0.0); // Start visible
            txtaChat.setVisible(false);
          }
          if (txtInput != null) {
            txtInput.setTranslateY(0.0);
            txtInput.setVisible(false);
          }
          if (btnSend != null) {
            btnSend.setTranslateY(0.0);
            btnSend.setVisible(false);
          }

          // Reset dropdown arrow to bottom pos
          if (dropUpArrow != null) {
            dropUpArrow.setVisible(false);
            dropUpArrow.setLayoutX(14.0);
            dropUpArrow.setLayoutY(540.0);
          }

          // Show next button for flashbacks
          if (nextButton != null) {
            nextButton.setVisible(true);
          }

          // Reset back button
          if (backBtn != null) {
            backBtn.setDisable(true);
          }

          // Reset slider
          if (unlockSlider != null) {
            unlockSlider.setVisible(false);
            unlockSlider.setDisable(false);
            unlockSlider.setValue(0.0);
          }

          // Reset flashback slideshow to first image
          if (flashbackSlideshow != null && !images.isEmpty()) {
            flashbackSlideshow.setImage(images.get(0));
          }
        });
  }
}
