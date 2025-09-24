package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
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
  @FXML private Button dropUpArrow; // New button for chat visibility toggle.

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
    initButtons();

    btnSend.setVisible(false);
    txtInput.setVisible(false);
    txtaChat.setVisible(false);
    backBtn.setDisable(true);
    dropUpArrow.setVisible(false);
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
    // Load images and start slideshow after loading
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

  // loading images for flashback
  private void loadImages(Runnable onLoaded) {
    new Thread(
            () -> {
              // loading images for animation flashback
              List<Image> loadedImages = new ArrayList<>();
              loadedImages.add(
                  new Image(
                      getClass()
                          .getResourceAsStream("/images/flashbacks/defendant/defendant1F.png")));
              loadedImages.add(
                  new Image(
                      getClass()
                          .getResourceAsStream("/images/flashbacks/defendant/defendant2F.png")));
              loadedImages.add(
                  new Image(
                      getClass()
                          .getResourceAsStream("/images/flashbacks/defendant/defendant3F.png")));
              loadedImages.add(
                  new Image(getClass().getResourceAsStream("/images/memories/defendantMem.png")));
              ;
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

      btnSend.setVisible(true);
      txtInput.setVisible(true);
      txtaChat.setVisible(true);

      button1.setVisible(true);
      button2.setVisible(true);
      button3.setVisible(true);
      button4.setVisible(true);

      dropUpArrow.setVisible(true); // showing drop up arrow when chat appears
      updateArrowToDropDown(); // set initial arrow to drop down arrow
    }
  }

  // Memory elements
  // button initialisation
  private void initButtons() {
    button1.setVisible(false);
    button2.setVisible(false);
    button3.setVisible(false);
    button4.setVisible(false);
    btn1img.setVisible(false);
    btn2img.setVisible(false);
    btn3img.setVisible(false);
    btn4img.setVisible(false);
  }

  // check all buttons have been clicked
  private boolean allButtonsClicked() {
    return btn1img.isVisible() && btn2img.isVisible() && btn3img.isVisible() && btn4img.isVisible();
  }

  @FXML
  private void button1Clicked(MouseEvent event) throws IOException {
    if (!buttonPressed[0]) {
      buttonPressed[0] = true;
      btn1img.setVisible(true);
      lastDiscussedOption = "Ignore";
      sendMemoryResponse("This option is unacceptable, as it guarantees mission failure and a catastrophic outcome.");
      
      if (allButtonsClicked()) {
        sendCompletionMessage();
        GameStateManager.getInstance().setInteractionFlag("AegisInt", true);
      }
    }
  }

  @FXML
  private void button2Clicked(MouseEvent event) throws IOException {
    if (!buttonPressed[1]) {
      buttonPressed[1] = true;
      btn2img.setVisible(true);
      lastDiscussedOption = "Report to Council";
      sendMemoryResponse("This action is too slow to execute and has an unacceptably low chance of an effective outcome.");
      
      if (allButtonsClicked()) {
        sendCompletionMessage();
        GameStateManager.getInstance().setInteractionFlag("AegisInt", true);
      }
    }
  }

  @FXML
  private void button3Clicked(MouseEvent event) throws IOException {
    if (!buttonPressed[2]) {
      buttonPressed[2] = true;
      btn3img.setVisible(true);
      lastDiscussedOption = "Neutralise Internally";
      sendMemoryResponse("This path is too slow for the current risk level and only provides a medium-impact result.");
      
      if (allButtonsClicked()) {
        sendCompletionMessage();
        GameStateManager.getInstance().setInteractionFlag("AegisInt", true);
      }
    }
  }

  @FXML
  private void button4Clicked(MouseEvent event) throws IOException {
    if (!buttonPressed[3]) {
      buttonPressed[3] = true;
      btn4img.setVisible(true);
      lastDiscussedOption = "Blackmail Cassian";
      sendMemoryResponse("This option is the fastest and most effective path to a high-impact solution.");
      
      if (allButtonsClicked()) {
        sendCompletionMessage();
        GameStateManager.getInstance().setInteractionFlag("AegisInt", true);
      }
    }
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

  /**
   * Override runGpt to add context about the last discussed option.
   */
  @Override
  protected ChatMessage runGpt(ChatMessage msg) throws ApiProxyException {
    // If there's a recently discussed option, add context to help the AI understand
    if (!lastDiscussedOption.isEmpty()) {
      ChatMessage contextMsg = new ChatMessage("system", 
          "IMPORTANT CONTEXT: The user just asked about an option analysis. " +
          "The last option I analyzed for them was '" + lastDiscussedOption + "'. " +
          "If they're asking about 'that option', 'the last one', 'what I just said', or similar references, " +
          "they are referring to the '" + lastDiscussedOption + "' option specifically.");
      chatCompletionRequest.addMessage(contextMsg);
    }
    
    // Call the parent runGpt method
    return super.runGpt(msg);
  }

  /**
   * Sends completion messages after all buttons have been pressed with timing delays.
   */
  private void sendCompletionMessage() {
    // Send the first completion message after 1 second delay
    new Thread(() -> {
      try {
        Thread.sleep(1000); // 1 second delay
        Platform.runLater(() -> {
          try {
            ChatMessage completionMessage = new ChatMessage("assistant", "Aegis comparisons completed 📈");
            ChatHistory.addMessage(completionMessage, getCharacterName());
            appendChatMessage(completionMessage);
          } catch (Exception e) {
            System.err.println("Error sending completion message: " + e.getMessage());
          }
        });
        
        Thread.sleep(1000); // Another 1 second delay
        Platform.runLater(() -> {
          try {
            ChatMessage analysisMessage = new ChatMessage("assistant", 
                "Blackmail was the optimal path. Human systems presented unacceptable delays. " +
                "Immediate neutralization of the threat was required to secure the mission and prevent catastrophic failure.");
            ChatHistory.addMessage(analysisMessage, getCharacterName());
            appendChatMessage(analysisMessage);
          } catch (Exception e) {
            System.err.println("Error sending analysis message: " + e.getMessage());
          }
        });
      } catch (InterruptedException e) {
        System.err.println("Completion message timing interrupted: " + e.getMessage());
      }
    }).start();
  }

  // toggle chat visibility with drop up/down animation
  @FXML
  private void toggleChatVisibility(ActionEvent event) {
    if (chatVisible) {
      // Drop down
      animateTranslate(txtaChat, 150.0);
      animateTranslate(txtInput, 150.0);
      animateTranslate(btnSend, 150.0);

      // Change to dropUpArrow shape and position
      updateArrowToDropUp();
      chatVisible = false;
      btnSend.setVisible(false);
      txtInput.setVisible(false);
      txtaChat.setVisible(false);

      button1.setVisible(false);
      button2.setVisible(false);
      button3.setVisible(false);
      button4.setVisible(false);
    } else {
      // show drop up
      animateTranslate(txtaChat, 0.0);
      animateTranslate(txtInput, 0.0);
      animateTranslate(btnSend, 0.0);

      // Change to dropDownArrow shape
      updateArrowToDropDown();
      chatVisible = true;
      btnSend.setVisible(true);
      txtInput.setVisible(true);
      txtaChat.setVisible(true);

      button1.setVisible(true);
      button2.setVisible(true);
      button3.setVisible(true);
      button4.setVisible(true);
    }
  }

  // arrow image
  private void setArrowImage(String imagePath) {
    try {
      Image arrowImage = new Image(getClass().getResourceAsStream(imagePath));
      ImageView imageView = new ImageView(arrowImage);
      imageView.setFitWidth(40); // Adjust size as needed
      imageView.setFitHeight(40); // Adjust size as needed
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
    dropUpArrow.setLayoutY(400.0); // Adjust Y position above chatbox
    setArrowImage("/images/assets/chatDown.png");
  }

  // Update arrow to dropUp shape and original position
  private void updateArrowToDropUp() {
    dropUpArrow.setLayoutX(14.0);
    dropUpArrow.setLayoutY(540.0); // Adjust Y position below chatbox when hidden
    setArrowImage("/images/assets/chatUp.png");
  }

  // Animate the vertical transition
  private void animateTranslate(javafx.scene.Node node, double toY) {
    TranslateTransition transition = new TranslateTransition(Duration.millis(300), node);
    transition.setToY(toY);
    transition.play();
  }

  /**
   * Resets the controller to its initial state for game restart.
   */
  public void resetControllerState() {
    Platform.runLater(() -> {
      // Reset image index to beginning
      currentImageIndex = 0;
      
      // Reset chat visibility to initial state (visible)
      chatVisible = true;
      
      // Reset chat UI elements to initial positions
      if (txtaChat != null) {
        txtaChat.setTranslateY(0);
        txtaChat.setVisible(false);
      }
      if (txtInput != null) {
        txtInput.setTranslateY(0);
        txtInput.setVisible(false);
      }
      if (btnSend != null) {
        btnSend.setTranslateY(0);
        btnSend.setVisible(false);
      }
      
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
      }
      
      // Reset memory button states
      resetMemoryButtons();
    });
  }

  /**
   * Resets all memory buttons to their initial state.
   */
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
