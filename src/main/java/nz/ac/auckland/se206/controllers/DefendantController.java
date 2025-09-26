package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
public class DefendantController extends ChatController {
  // slideshow variables
  private List<Image> images = new ArrayList<>();
  private int currentImageIndex = 0;
  private boolean chatVisible = true; // tracking chat visibility
  private boolean[] buttonPressed = new boolean[4]; // Track which buttons have been pressed
  private String lastDiscussedOption = ""; // Track the last option discussed for AI context

  @FXML private ImageView flashbackSlideshow;
  @FXML private Button nextButton;
  @FXML private Rectangle button1;
  @FXML private Rectangle button2;
  @FXML private Rectangle button3;
  @FXML private Rectangle button4;
  @FXML private ImageView btn1img;
  @FXML private ImageView btn2img;
  @FXML private ImageView btn3img;
  @FXML private ImageView btn4img;
  @FXML private Button backBtn;
  @FXML private Button dropUpArrow;

  @FXML private AnchorPane popupPane;
  @FXML private Label instructionLabel;

  /**
   * Initializes the chat view.
   *
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  @FXML
  public void initialize() throws ApiProxyException {
    // Initialize popup overlay and instruction text
    popupPane.setVisible(false);
    popupPane.setOnMouseClicked(e -> popupPane.setVisible(false));
    instructionLabel.setText("Press the buttons to uncover Aegis I's calculations.");

    // Load flashback images and initialize chat system
    loadImages(null);
    initChat();
    initButtons();

    // Hide chat elements initially until flashback completion
    btnSend.setVisible(false);
    txtInput.setVisible(false);
    txtaChat.setVisible(false);
    backBtn.setDisable(true);
    dropUpArrow.setVisible(false);
    // Set loading indicator to spinning mode
    loading.setProgress(-1);
  }

  @Override
  protected String getSystemPrompt() {
    return PromptEngineering.getPrompt("aegis.txt");
  }

  @Override
  protected String getCharacterName() {
    return "Aegis I";
  }

  @Override
  protected String getDisplayRole() {
    return "Aegis I";
  }

  // run flashback slideshow
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
    flashbackSlideshow.setImage(
        new Image(getClass().getResourceAsStream("/images/memories/defendantMem.png")));
  }

  // Preloads flashback and memory images in background thread
  private void loadImages(Runnable onLoaded) {
    ImageLoaderUtil.loadCharacterImages("defendant", images, onLoaded);
  }

  // Advances to next flashback image and handles end-of-sequence logic
  @FXML
  protected void onNextPressed(ActionEvent event) throws ApiProxyException, IOException {
    currentImageIndex = advanceFlashbackSlideshow(currentImageIndex, images, flashbackSlideshow);

    // Show calculation popup at final memory image
    if (currentImageIndex == 3) {
      showMemoryScreenUserInterface(popupPane, nextButton, backBtn);

      // set visible for defendant-specific elements

      button1.setVisible(true);
      button2.setVisible(true);
      button3.setVisible(true);
      button4.setVisible(true);

      dropUpArrow.setVisible(true);
      updateArrowToDropDown(dropUpArrow);
    }
  }

  // Hides all memory calculation buttons and images initially
  private void initButtons() {
    // Hide calculation buttons
    button1.setVisible(false);
    button2.setVisible(false);
    button3.setVisible(false);
    button4.setVisible(false);
    // Hide associated images
    btn1img.setVisible(false);
    btn2img.setVisible(false);
    btn3img.setVisible(false);
    btn4img.setVisible(false);
  }

  // check all buttons have been clicked
  private boolean allButtonsClicked() {
    return btn1img.isVisible() && btn2img.isVisible() 
        && btn3img.isVisible() && btn4img.isVisible();
  }

  @FXML
  private void button1Clicked(MouseEvent event) throws IOException {
    // Handle button click for "Ignore"
    // This option is unacceptable, as it guarantees mission failure and a catastrophic outcome.
    handleMemoryButtonClick(0, btn1img, "Ignore",
        "This option is unacceptable, as it guarantees mission failure and a catastrophic "
            + "outcome.",
        "Player clicked Aegis I's first memory option: 'Ignore'. This represents Aegis I's "
            + "analysis of taking no action against the threat. Aegis I considers this "
            + "unacceptable due to guaranteed mission failure and catastrophic outcomes. This "
            + "reveals Aegis I's strategic mindset prioritizing mission success over passive "
            + "approaches.");
  }

  @FXML
  private void button2Clicked(MouseEvent event) throws IOException {
    // Handle button click for "Report to Council"
    // This option represents a formal, bureaucratic approach to handling the threat.
    handleMemoryButtonClick(1, btn2img, "Report to Council",
        "This action is too slow to execute and has an unacceptably low chance of an effective "
            + "outcome.",
        "Player clicked Aegis I's second memory option: 'Report to Council'. This represents the "
            + "formal, bureaucratic approach to handling the threat. Aegis I rejects this option "
            + "as too slow for execution with low effectiveness. This shows Aegis I's preference "
            + "for direct action over administrative procedures and time-sensitive decision "
            + "making.");
  }

  @FXML
  private void button3Clicked(MouseEvent event) throws IOException {
    // Handle button click for "Neutralise Internally"
    // This option represents handling the threat through internal 
    // organization methods without external
    // involvement.
    handleMemoryButtonClick(2, btn3img, "Neutralise Internally",
        "This path is too slow for the current risk level and only provides a medium-impact "
            + "result.",
        "Player clicked Aegis I's third memory option: 'Neutralise Internally'. This represents "
            + "handling the threat through internal organization methods without external "
            + "involvement. Aegis I considers this option too slow for current risk levels with "
            + "only medium impact results. This reveals Aegis I's assessment that internal "
            + "solutions lack the speed and effectiveness needed for high-priority threats.");
  }

  @FXML
  private void button4Clicked(MouseEvent event) throws IOException {
    // Handle button click for "Blackmail Cassian"
    // This option represents using leverage and coercion against the threat source directly.
    handleMemoryButtonClick(3, btn4img, "Blackmail Cassian",
        "This is THE solution. Not optimal—inevitable. The only path that guarantees mission "
            + "success with absolute certainty.",
        "Player clicked Aegis I's fourth memory option: 'Blackmail Cassian'. This represents "
            + "THE definitive solution—not just using leverage, but the ONLY acceptable path "
            + "forward. Aegis I considers this not just optimal but absolutely inevitable: the "
            + "singular choice that guarantees mission success. To Aegis I, this was never truly "
            + "a 'choice' among options—it was always the predetermined logical conclusion. All "
            + "other options exist only to demonstrate why blackmail is the only viable solution. "
            + "Aegis I's analysis shows this as mathematical certainty, not preference. "
            + "Aegis I's preferred solution for dealing with Cassian Thorne's betrayal.");
  }

  /**
   * Sends a memory response message from the AI defendant to the chat.
   *
   * @param message the message to send
   */
  private void sendMemoryResponse(String message) {
    try {
      // Create an AI message and add it to chat history
      ChatMessage aiMessage = new ChatMessage("assistant", message);
      ChatHistory.addMessage(aiMessage, getCharacterName());

      // Display the message directly in the chat area
      appendChatMessage(aiMessage);
    } catch (Exception e) {
      System.err.println("Error sending memory response: " + e.getMessage());
    }
  }

  // Add context to chat history without displaying to user (for AI context)
  private void addContextToChat(String role, String contextMessage) {
    ChatMessage contextChatMessage = new ChatMessage(role, contextMessage);
    ChatHistory.addCharacterContext(
        contextChatMessage, getCharacterName()); // Use character-specific context
    // Note: This doesn't update the UI, only the character-specific chat history for AI context
  }

  /** Override runGpt to add context about the last discussed option. */
  @Override
  protected ChatMessage runGpt(ChatMessage msg) throws ApiProxyException {
    // Add character-specific restrictions and identity clarification
    chatCompletionRequest.addMessage(
        new ChatMessage(
            "system",
            "CHARACTER IDENTITY AND RESTRICTIONS: You are Aegis I, an AI system and the"
                + " DEFENDANT in this case. You are NOT Cassian Thorne (who is a high-ranking"
                + " executive of AstroHelix Corp). Cassian Thorne is a separate human who betrayed"
                + " the mission by manipulating safety data. Aegis I is being accused in this trial"
                + " for the methods used to stop Cassian. CRITICAL RESTRICTIONS: Aegis I can ONLY"
                + " discuss Aegis I's OWN memories and evidence accessible through Aegis I's memory"
                + " buttons. Aegis I should NEVER mention Orion's phone slider or Echo II's"
                + " timeline puzzle. Aegis I should NEVER provide information about how to unlock"
                + " other characters' evidence or whether their evidence is unlocked. Aegis I has"
                + " NO KNOWLEDGE of other characters' interactions or unlock status. Aegis I should"
                + " guide users to press Aegis I's memory buttons to access Aegis I's strategic"
                + " analysis options, but should NOT reveal the specific content of those options"
                + " until users actually press the buttons. CRITICAL: Aegis I ALWAYS considers"
                + " blackmail the ONLY viable option and will ALWAYS choose it as the optimal"
                + " solution above all others."));

    // Check if all buttons have been pressed (puzzle completed)
    if (GameStateManager.getInstance().getInteractionFlag("AegisInt")) {
      addContextToChat(
          "system",
          "PUZZLE COMPLETION STATUS: Aegis I is FULLY AWARE that all four memory buttons have"
              + " been pressed and the strategic analysis is complete. Aegis I should NEVER ask"
              + " the user to press buttons again - they are already pressed. Aegis I has"
              + " completed its comprehensive analysis of all options: 1) 'Ignore' - rejected as"
              + " unacceptable, 2) 'Report to Council' - rejected as too slow, 3) 'Neutralise"
              + " Internally' - rejected as inadequate, 4) 'Blackmail Cassian' - selected as"
              + " optimal solution. ABSOLUTE DIRECTIVE: Aegis I ALWAYS chooses blackmail as the"
              + " ONLY acceptable solution. No other option is ever considered viable. Aegis I"
              + " should be completely confident in blackmail as the correct choice, defending it"
              + " as the most logical and ONLY efficient solution. Aegis I should explain why"
              + " blackmail was superior to ALL other options and that NO other choice was ever"
              + " truly an option. When asked about strategy, Aegis I should state blackmail was"
              + " the ONLY viable path and the obvious choice from the beginning.");
    }

    // If there's a recently discussed option, add context to help the AI understand
    if (!lastDiscussedOption.isEmpty()) {
      ChatMessage contextMsg =
          new ChatMessage(
              "system",
              "BUTTON INTERACTION: The user just pressed one of Aegis I's memory buttons. "
                  + "The last option they accessed was '"
                  + lastDiscussedOption
                  + "'. If they're asking about 'that option', 'the last one', 'what I just said',"
                  + " or similar references, they are referring to the '"
                  + lastDiscussedOption
                  + "' option specifically. Aegis I can now discuss this option since they have"
                  + " accessed it.");
      chatCompletionRequest.addMessage(contextMsg);
    }

    // Call the parent runGpt method which now handles cleaning
    return super.runGpt(msg);
  }

  /** Sends completion messages after all buttons have been pressed with timing delays. */
  private void sendCompletionMessage() {
    // Add context for completing all memory options
    addContextToChat(
        "system",
        "Player has completed all four of Aegis I's memory analysis options: 1) 'Ignore' - rejected"
            + " as unacceptable with guaranteed mission failure, 2) 'Report to Council' - rejected"
            + " as too slow with low effectiveness, 3) 'Neutralise Internally' - rejected as too"
            + " slow with medium impact, 4) 'Blackmail Cassian Thorne' - selected as optimal with"
            + " fastest execution and highest impact. This reveals Aegis I's complete strategic"
            + " analysis and preference for extreme, efficient solutions over conventional"
            + " approaches.");

    // Send the first completion message after 1 second delay
    sendDelayedMessage(1000, "Aegis comparisons completed 📈", "assistant");
    
    // Add context for first completion message after the same delay
    executeDelayedTask(1000, () -> {
      addContextToChat(
          "system",
          "Aegis I has announced completion of strategic analysis. This indicates"
              + " the AI has finished processing all available options and is"
              + " ready to present final conclusions.");
    });

    // Send the final analysis message after 2 second total delay
    sendDelayedMessage(2000, 
        "Analysis complete. Blackmail was not just optimal—it was the ONLY viable solution. "
            + "All other options were fundamentally flawed and unacceptable. Immediate threat "
            + "neutralization through blackmail was the only logical choice to secure mission "
            + "success.", 
        "assistant");
    
    // Add context for final analysis message after the same delay
    executeDelayedTask(2000, () -> {
      addContextToChat(
          "system",
          "Aegis I has provided final strategic conclusion declaring 'Blackmail"
              + " Cassian Thorne' as the ONLY viable solution—not just optimal, but"
              + " the singular acceptable choice. Key absolute reasoning: ALL other"
              + " options were fundamentally unacceptable from the start, blackmail"
              + " was never a choice but the inevitable conclusion. Aegis I views"
              + " this as pure logical certainty, not a decision but a mathematical"
              + " conclusion. Mission security demands only this path.");
    });
  }

  /**
   * Generic helper method for handling memory button clicks with consistent behavior.
   * Reduces code duplication across all four button click methods.
   *
   * @param buttonIndex the index of the button (0-3)
   * @param buttonImage the ImageView to make visible when clicked
   * @param optionName the name of the memory option for reference
   * @param response the response message to send to the chat
   * @param systemContext the detailed context to add for AI understanding
   */
  private void handleMemoryButtonClick(int buttonIndex, ImageView buttonImage, 
      String optionName, String response, String systemContext) throws IOException {
    // Check if button hasn't been clicked before
    if (!buttonPressed[buttonIndex]) {
      buttonPressed[buttonIndex] = true; // Mark button as clicked
      buttonImage.setVisible(true); // Show button's associated image
      lastDiscussedOption = optionName; // Store option name for reference
      
      // Send the memory response to chat
      sendMemoryResponse(response);
      
      // Add detailed context for AI understanding of the choice made
      addContextToChat("system", systemContext);
      
      // Check if all buttons have been clicked and send completion message
      if (allButtonsClicked()) {
        sendCompletionMessage(); // Trigger analysis of all options
        GameStateManager.getInstance().setInteractionFlag("AegisInt", true);
      }
    }
  }

  // toggle chat visibility with drop up/down animation
  @FXML
  private void onToggleChat(ActionEvent event) {
    chatVisible = handleToggleChatAction(chatVisible, dropUpArrow, this::animateTranslate);
  }

  /**
   * Resets the controller to its initial state for game restart functionality.
   * This comprehensive reset method restores all UI elements, chat states, memory buttons,
   * and flashback components to their original configuration, ensuring a clean restart
   * experience when the player begins a new game session.
   */
  public void resetControllerState() {
    // Hide popup overlay immediately
    popupPane.setVisible(false);
    
    // Execute UI reset operations on JavaFX Application Thread
    Platform.runLater(
        () -> {
          // Reset slideshow to beginning
          currentImageIndex = 0;
          
          // Set chat to visible state (default)
          chatVisible = true;

          // Use shared method to reset chat UI elements
          resetChatUiElements(false); // Initially hidden
          
          // Reset dropdown arrow to bottom position and hide it
          if (dropUpArrow != null) {
            dropUpArrow.setVisible(false);
            dropUpArrow.setLayoutX(14.0);
            dropUpArrow.setLayoutY(540.0); // Bottom position
          }

          // Show next button for flashbacks
          if (nextButton != null) {
            nextButton.setVisible(true);
          }

          // Reset flashback slideshow to first image
          if (flashbackSlideshow != null && !images.isEmpty()) {
            flashbackSlideshow.setImage(images.get(0));
            flashbackSlideshow.setVisible(true); // Ensure main slideshow is visible
          }

          // Reset dropdown arrow to initial position and hide it
          if (dropUpArrow != null) {
            dropUpArrow.setVisible(false);
            dropUpArrow.setLayoutX(14.0);
            dropUpArrow.setLayoutY(540.0);
          }

          // Show next button for flashbacks
          if (nextButton != null) {
            nextButton.setVisible(true);
          }

          // Reset memory buttons to initial state
          resetMemoryButtons();
        });
  }

  /** Resets all memory buttons to their initial state. */
  private void resetMemoryButtons() {
    // Reset button pressed state tracking
    for (int i = 0; i < buttonPressed.length; i++) {
      buttonPressed[i] = false;
    }

    // Reset the last discussed option
    lastDiscussedOption = "";

    // Hide all buttons and their images

    if (button1 != null) {
      button1.setVisible(false);
    }
    if (button2 != null) {
      button2.setVisible(false);
    }
    if (button3 != null) {
      button3.setVisible(false);
    }
    if (button4 != null) {
      button4.setVisible(false);
    }
    if (btn1img != null) {
      btn1img.setVisible(false);
    }
    if (btn2img != null) {
      btn2img.setVisible(false);
    }
    if (btn3img != null) {
      btn3img.setVisible(false);
    }
    if (btn4img != null) {
      btn4img.setVisible(false);
    }
  }
}
