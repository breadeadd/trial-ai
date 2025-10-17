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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
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
  private boolean notifInteract = false;

  @FXML private ImageView flashbackSlideshow;
  @FXML private Rectangle screenBox;
  @FXML private Button nextButton;
  @FXML private Slider unlockSlider;
  @FXML private Button dropUpArrow;

  @FXML private Button backBtn;
  @FXML private ImageView notif;
  @FXML private ImageView notifBig;

  @FXML private AnchorPane popupPane;
  @FXML private Label instructionLabel;

  @FXML private Button closePopupBtn;

  @FXML
  private void onClosePopup(ActionEvent event) {
    popupPane.setVisible(false);
  }

  /**
   * Initializes the chat view.
   *
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  @FXML
  public void initialize() throws ApiProxyException {
    popupPane.setVisible(false);
    // popupPane.setOnMouseClicked(e -> popupPane.setVisible(false));
    instructionLabel.setText("Investigate to find Cassian Thorne's messages.");
    loadImages(null);
    initChat();

    // Hide chat UI elements initially
    setChatUiVisibility(false);
    backBtn.setDisable(true);
    loading.setProgress(-1);

    unlockSlider.setVisible(false); // Slider is initially hidden
    dropUpArrow.setVisible(false); // Drop up arrow initially hidden

    // notifications
    notif.setVisible(false);
    notifBig.setVisible(false);
    // Bind the computer screen interactable: single handler for screen inspection
    if (screenBox != null) {
      screenBox.setOnMouseClicked(this::onScreenInteract);
    }
  }

  // Single handler for computer screen interaction
  @FXML
  private void onScreenInteract(MouseEvent event) {
    // Mark that the player inspected the screen
    GameStateManager.getInstance().setInteractionFlag("OrionScreen", true);

    // Append visible user message so chat shows "You: Computer Screen Inspected 🔎"
    ChatMessage screenUserMsg = new ChatMessage("user", "Computer Screen Inspected 🔎");
    appendChatMessage(screenUserMsg);

    // Add two system-context entries to provide AI with detailed context about the inspection
    addContextToChat(
        "system",
        "COMPUTER SCREEN INSPECTION CONTEXT: The player inspected a computer terminal's"
            + " screen (desktop client / mail client) that displayed an email from AstroHelix."
            + " When opened the email showed: 'Hi AstroHelix, we will invest $1m if Project"
            + " Starlight's green light can be given by the end of this month.' Treat this as an"
            + " email (not a phone notification) and as independent corroborating evidence from"
            + " the phone contents. The Orion should recognise terms like 'email', 'mail client',"
            + " 'inbox', and 'email preview' when referencing this evidence.");

    addContextToChat(
        "system",
        "INVESTIGATION AWARENESS: The player actively inspected the computer screen and"
            + " opened an email in the desktop client. Orion should explicitly acknowledge the"
            + " email inspection in his responses and reference the email contents or mail"
            + " client logs when discussing evidence. When referring to the human interacting"
            + " with the UI, use the term 'player' (or 'user') rather than 'investigator'.");

    // Schedule a short assistant acknowledgement
    executeDelayedTask(
        800,
        () ->
            displayMessage(
                "I saw those logs too. They mention unauthorized access tied to Cassian — that's"
                    + " serious."));
    // After a short delay, if the phone hasn't been unlocked yet, nudge the player to check it
    executeDelayedTask(
        2000,
        () -> {
          if (!GameStateManager.getInstance().getInteractionFlag("OrionInt")) {
            displayMessage(
                "You should probably unlock my phone next — there may be messages from"
                    + " the CEO that explain motives related to Project Starlight.");
          }
        });
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
    // Use shared slideshow initialization method for consistent behavior
    initializeFlashbackSlideshow(images, flashbackSlideshow, this::startFlashbackSlideshow);
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
      if (notifInteract) {
        notifBig.setVisible(true);
      } else {
        notif.setVisible(true);
        notif.setDisable(false);
      }
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
      notif.setVisible(true);
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

  // notification transition
  @FXML
  private void notifPressed(MouseEvent event) {
    // Notification was clicked: show a user-visible "You:" message, add system context,
    // and schedule a short assistant acknowledgement — mirror phone-unlock flow.
    notifInteract = true;
    notif.setVisible(false);
    notifBig.setVisible(true);

    // Mark that the player inspected the computer email via the notification
    GameStateManager.getInstance().setInteractionFlag("OrionScreen", true);

    // Append visible user message so chat shows "You: Notification Viewed 🔎"
    ChatMessage notifUserMsg = new ChatMessage("user", "Notification Viewed 🔎");
    appendChatMessage(notifUserMsg);

    // Add two system-context entries (descriptive + investigation awareness) for AI
    // Explain that the on-screen preview was partial and tapping revealed the full message
    addContextToChat(
        "system",
        "NOTIFICATION VIEWED CONTEXT: A truncated notification preview from AstroHelix was"
            + " visible on the computer screen before the player tapped it (the preview showed a"
            + " short snippet referencing Project Starlight and an investment). When the player"
            + " tapped the preview, the full message was revealed and reads: 'Hi AstroHelix, we"
            + " will invest $1m if Project Starlight's green light can be given by the end of this"
            + " month.' This revealed message suggests possible financial leverage or negotiation"
            + " around Project Starlight and may corroborate other evidence referring to Cassian"
            + " Thorne.");

    addContextToChat(
        "system",
        "INVESTIGATION AWARENESS: The notification preview is a distinct interactive event"
            + " from the phone slider unlock. Orion should acknowledge that the preview was"
            + " initially partial and that the player revealed the full notification by tapping it."
            + " Orion should reference the full contents (including the AstroHelix offer and"
            + " Project Starlight mention) as corroborating evidence when appropriate. The"
            + " notification could indicate financial pressure or leverage tied to Project"
            + " Starlight.");

    // Schedule a short assistant acknowledgement referencing the notification
    executeDelayedTask(
        800,
        () ->
            displayMessage(
                "It mentions AstroHelix and an investment tied to"
                    + " Project Starlight. That could explain why someone might try to use this as"
                    + " leverage."));

    // After a short delay, if the phone hasn't been unlocked yet, nudge the player to check it
    executeDelayedTask(
        2000,
        () -> {
          if (!GameStateManager.getInstance().getInteractionFlag("OrionInt")) {
            displayMessage(
                "You should probably unlock my phone next — there may be messages from"
                    + " the CEO that explain motives related to Project Starlight.");
          }
        });
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
    executeDelayedTask(
        1000,
        () -> {
          displayMessage(
              "So Cassian compromised the mission, which makes Aegis's reaction to"
                  + " protect it understandable. But its methods were EXTREME. Cassian"
                  + " could've been in action for good.");
        });
    // After a short delay, if the computer email has not been inspected, urge the player to
    // open the computer/email interactable to check for corroborating messages.
    executeDelayedTask(
        2200,
        () -> {
          if (!GameStateManager.getInstance().getInteractionFlag("OrionScreen")) {
            displayMessage(
                "There is an email preview on the computer screen — you should open the"
                    + " computer email to see if it contains related information about Project"
                    + " Starlight.");
          }
        });
  }

  // Mirror phone flow: send notification viewed messages with timing

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
    chatVisible = handleToggleChatAction(chatVisible, dropUpArrow, this::animateTranslate);
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

          // reset notification
          notif.setVisible(false);
          notifBig.setVisible(false);
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
                + " worked on the mission and has evidence on your phone. IMPORTANT: Orion can"
                + " discuss his own phone slider mechanism. Orion must NOT disclose private unlock"
                + " methods or puzzle solutions belonging to other characters. If directly asked"
                + " about another character's puzzle, Orion may describe the general topic at a"
                + " high level but must refuse to provide unlock steps or explicit answers and"
                + " should instruct the asker to speak with that puzzle's owner by name (for"
                + " example: 'Ask Echo II' or 'Ask Aegis I') to get further details."));

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
                  + " protocols, and Project Starlight intelligence. Messages on the phone indicate"
                  + " that the CEO prioritised financial gain over crew safety — financial"
                  + " incentives were often placed above safety concerns. I should acknowledge that"
                  + " the player has already seen this evidence on my unlocked phone."));

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

    // Add computer screen context if the investigator inspected the computer screen.
    // This is distinct from the phone unlock interaction and should be treated separately.
    if (GameStateManager.getInstance().getInteractionFlag("OrionScreen")) {
      chatCompletionRequest.addMessage(
          new ChatMessage(
              "assistant",
              "CRITICAL: I am Orion Vale and I ACKNOWLEDGE that the player inspected a computer"
                  + " screen which displayed an email in the desktop client. The email referenced"
                  + " Project Starlight and an investment offer. This email is independent evidence"
                  + " from the phone contents. When discussing the investigation I should"
                  + " explicitly reference the computer email and avoid conflating it with the"
                  + " phone unlock or its messages."));

      chatCompletionRequest.addMessage(
          new ChatMessage(
              "system",
              "COMPUTER SCREEN INSPECTION STATUS: The player has inspected the computer's"
                  + " screen and viewed an email in the desktop client from AstroHelix referencing"
                  + " Project Starlight and an investment offer. This email is independent from the"
                  + " phone evidence. Orion is aware of the email and should treat it as separate"
                  + " corroborating evidence when relevant to the conversation."));
    } else {
      chatCompletionRequest.addMessage(
          new ChatMessage(
              "system",
              "SCREEN NOTICE: The computer screen currently displays a partial email preview"
                  + " (subject/preview snippet) from the desktop mail client referencing Project"
                  + " Starlight. The full email (including an investment offer) is only visible"
                  + " when the player opens the email; opening reveals the complete message. This"
                  + " email preview should be treated as separate evidence from the phone and may"
                  + " indicate financial pressure or negotiation related to Project Starlight."));
    }

  // Delegate to the ChatController helper using the captured request instance
  return runGptWithRequest(chatCompletionRequest, msg);
  }
}
