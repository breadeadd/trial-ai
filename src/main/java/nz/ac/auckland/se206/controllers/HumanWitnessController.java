package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.ChatHistory;
import nz.ac.auckland.se206.prompts.PromptEngineering;
import nz.ac.auckland.se206.states.GameStateManager;
import nz.ac.auckland.se206.util.ImageLoaderUtil;

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

  @FXML private AnchorPane popupPane;
  @FXML private Label instructionLabel;

  /**
   * Initializes the chat view.
   *
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  @FXML
  public void initialize() throws ApiProxyException {
    popupPane.setVisible(false);
    popupPane.setOnMouseClicked(e -> popupPane.setVisible(false));
    instructionLabel.setText("Investigate to find Cassian Thorne's messages.");
    loadImages(null);
    initChat();

    // Hide chat UI elements initially
    setChatUiVisibility(false);
    backBtn.setDisable(true);
    loading.setProgress(-1);

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
      updateArrowToDropDown(dropUpArrow);
    }
  }

  // Preloads human witness flashback sequence in background
  // Loads flashback and memory images in background thread
  private void loadImages(Runnable onLoaded) {
    ImageLoaderUtil.loadHumanWitnessImages(images, onLoaded);
  }

  // Change to next scene
  @FXML
  protected void onNextPressed(ActionEvent event) throws ApiProxyException, IOException {
    currentImageIndex = advanceFlashbackSlideshow(currentImageIndex, images, flashbackSlideshow);

    // flashback ends and chat begins
    if (currentImageIndex == 3) {
      showMemoryScreenUserInterface(popupPane, nextButton, backBtn);

      unlockSlider.setVisible(true);
      unlockSlider.setDisable(false);
      dropUpArrow.setVisible(true);
      updateArrowToDropDown(dropUpArrow); // Arrow starts pointing down

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
        updateArrowToDropDown(dropUpArrow);
      } else {
        updateArrowToDropUp(dropUpArrow);
      }
      // Show chat UI elements
      setChatUiVisibility(true);
    } else {
      dropUpArrow.setVisible(false); // Hide drop up arrow on other screens
      unlockSlider.setVisible(false);
      // Hide chat UI elements
      setChatUiVisibility(false);
    }
  }

  // Handle slider release to transition to humanMem2.png and hide slider
  // Handles slider release for phone unlock mechanism
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
        updateArrowToDropDown(dropUpArrow);
      } else {
        updateArrowToDropUp(dropUpArrow);
      }
      txtaChat.setVisible(chatVisible);
      txtInput.setVisible(chatVisible);
      btnSend.setVisible(chatVisible);

      // Add context for phone unlock event
      addContextToChat(
          "system",
          "PHONE UNLOCK CONTEXT: Player successfully unlocked Orion Vale's phone using the slider"
              + " mechanism. Orion is now fully aware that: 1) The player accessed his phone"
              + " through the unlock slider (not buttons), 2) The player has seen all the critical"
              + " mission intelligence stored on the phone, 3) The phone contained evidence of"
              + " Cassian's data manipulation, Aegis I's extreme protocols, and Project Starlight"
              + " compromise details. When discussing information or evidence, Orion should"
              + " reference the phone contents and acknowledge that the player has already accessed"
              + " this crucial evidence.");
      addContextToChat(
          "system",
          "INVESTIGATION AWARENESS: Orion knows the player is actively investigating through"
              + " interactive mechanisms. If asked about finding information in the future, Orion"
              + " should guide players toward other interactive elements rather than suggesting the"
              + " phone (which is now unlocked). The phone unlock demonstrates the player's"
              + " investigative approach and Orion should respond accordingly to their"
              + " evidence-gathering efforts.");

      // Send phone unlock messages with timing
      sendPhoneUnlockMessages();
    }
  }

  // Send phone unlock messages with timing
  // Displays unlock notification then Orion's story revelation
  private void sendPhoneUnlockMessages() {
    // Immediately show phone unlock message
    Platform.runLater(() -> displayMessage("Phone Unlocked 🔓"));

    // Send witness response after 1 second delay
    executeDelayedTask(1000, () -> {
      displayMessage(
          "So Cassian compromised the mission, which makes Aegis's reaction to"
              + " protect it understandable. But its methods were EXTREME. Cassian"
              + " could've been in action for good.");
    });
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
    ChatHistory.addCharacterContext(
        contextChatMessage, getCharacterName()); // Use character-specific context
    // Note: This doesn't update the UI, only the character-specific chat history for AI context
  }

  // Toggle chat visibility with drop-up/down animation
  @FXML
  private void onToggleChat(ActionEvent event) {
    chatVisible = toggleChatVisibility(chatVisible, this::animateTranslate);
    
    if (chatVisible) {
      // Change to dropDownArrow shape and position above chatbox
      updateArrowToDropDown(dropUpArrow);
    } else {
      // Change to dropUpArrow shape and move below
      updateArrowToDropUp(dropUpArrow);
    }
  }





  /** Resets the controller to its initial state for game restart. */
  public void resetControllerState() {
    popupPane.setVisible(false);
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

  @Override
  protected ChatMessage runGpt(ChatMessage msg) throws ApiProxyException {
    // Add character identity clarification
    chatCompletionRequest.addMessage(
        new ChatMessage(
            "system",
            "CHARACTER IDENTITY AND RESTRICTIONS: You are Orion Vale, the human witness in this"
                + " case. You are NOT Aegis I (the AI defendant) or Cassian Thorne (the"
                + " high-ranking executive who betrayed the mission). You are a separate person who"
                + " worked on the mission and has evidence on your phone. IMPORTANT: Orion can ONLY"
                + " discuss his own phone slider mechanism. Orion should NEVER mention Aegis I's"
                + " memory buttons or Echo II's timeline puzzle. Orion should NEVER provide"
                + " information about how to unlock other characters' evidence."));

    // Add phone status context based on current state
    if (GameStateManager.getInstance().getInteractionFlag("OrionInt")) {
      chatCompletionRequest.addMessage(
          new ChatMessage(
              "assistant",
              "CRITICAL: I am Orion Vale and I KNOW my phone has been unlocked. The investigator"
                  + " successfully used the slider mechanism to access my phone. I witnessed this"
                  + " happen and I am fully aware of it. I must NOT tell them to unlock the phone"
                  + " again - it's already unlocked. I must NOT ask for permission to access my"
                  + " phone - it's MY phone and it's already open. I should freely discuss what was"
                  + " found on my phone: the messages revealing Cassian Thorne's betrayal, the data"
                  + " showing he manipulated mission parameters, evidence of Aegis I's extreme"
                  + " protocols, and Project Starlight intelligence. I should acknowledge that the"
                  + " investigator has already seen this evidence on my unlocked phone."));

      chatCompletionRequest.addMessage(
          new ChatMessage(
              "system",
              "PHONE UNLOCKED STATUS: Orion Vale's phone slider has been completed by the player."
                  + " The phone is now unlocked and accessible. Orion is completely aware of this"
                  + " unlock event and should never suggest unlocking the phone again. Orion should"
                  + " NOT ask for permission to access his own phone since it's already unlocked"
                  + " and belongs to him. Instead, Orion should reference the evidence that was"
                  + " revealed on the phone and discuss it openly. The phone contained critical"
                  + " evidence about the mission betrayal."));
    } else {
      chatCompletionRequest.addMessage(
          new ChatMessage(
              "system",
              "PHONE STATUS: Orion's phone is currently locked and visible on screen. Orion should"
                  + " guide users to use the slider mechanism to unlock his phone, but should NOT"
                  + " reveal specific details about what's on the phone until they actually unlock"
                  + " it. Orion should only mention that his phone contains important evidence and"
                  + " messages that could help with the investigation. Orion should focus ONLY on"
                  + " his phone slider, not other characters' unlock methods."));
    }

    // Call the parent runGpt method which now handles cleaning
    return super.runGpt(msg);
  }
}
