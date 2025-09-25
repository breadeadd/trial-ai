package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
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
public class AiWitnessController extends ChatController {
  private List<Image> images = new ArrayList<>();
  private int currentImageIndex = 0;
  private boolean chatVisible = true; // Track chat visibility state
  private String lastTimelineAction = ""; // Track the last timeline action for AI context

  // Drag and drop variables
  private String[] slotContents = new String[3]; // Track what's in each slot
  private double[] originalX = new double[3]; // Store original positions
  private double[] originalY = new double[3];

  @FXML private ImageView flashbackSlideshow;
  @FXML private ImageView aiFlashback;
  @FXML private Button nextButton;
  @FXML private Button dropUpArrow;

  // Event images (draggable)
  @FXML private ImageView event1Image;
  @FXML private ImageView event2Image;
  @FXML private ImageView event3Image;

  // Drop slots
  @FXML private ImageView dropSlot1;
  @FXML private ImageView dropSlot2;
  @FXML private ImageView dropSlot3;

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
    instructionLabel.setText("Arrange the events to reconstruct Echo II's memory.");

    loadImages(null);
    initChat();

    btnSend.setVisible(false);
    txtInput.setVisible(false);
    txtaChat.setVisible(false);
    dropUpArrow.setVisible(false); // Drop up arrow initially hidden

    // Initialize drag and drop functionality
    setupDragAndDrop();

    // Store original positions of event images
    Platform.runLater(
        () -> {
          if (event1Image != null) {
            originalX[0] = event1Image.getLayoutX();
            originalY[0] = event1Image.getLayoutY();
          }
          if (event2Image != null) {
            originalX[1] = event2Image.getLayoutX();
            originalY[1] = event2Image.getLayoutY();
          }
          if (event3Image != null) {
            originalX[2] = event3Image.getLayoutX();
            originalY[2] = event3Image.getLayoutY();
          }
        });
  }

  /**
   * Generates the system prompt based on the profession.
   *
   * @return the system prompt string
   */
  @Override
  protected String getSystemPrompt() {
    return PromptEngineering.getPrompt("echo.txt");
  }

  @Override
  protected String getCharacterName() {
    return "Echo II";
  }

  @Override
  protected String getDisplayRole() {
    return "Echo II";
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

  // Not first time visiting
  public void runAfterFirst() {
    // Check if AI witness interaction has been completed
    if (GameStateManager.getInstance().getInteractionFlag("EchoInt") == true) {
      // If puzzle was completed, go directly to memory screen
      currentImageIndex = 3;
      if (!images.isEmpty()) {
        flashbackSlideshow.setImage(images.get(currentImageIndex));
      }

      // Show memory screen elements
      btnSend.setVisible(true);
      txtInput.setVisible(true);
      txtaChat.setVisible(true);
      dropUpArrow.setVisible(true);
      updateArrowToDropDown();
      nextButton.setVisible(false);

      // Show drag and drop elements
      showDragAndDropElements();
    } else {

      // If puzzle wasn't completed, show memory screen but allow puzzle interaction
      currentImageIndex = 3;
      if (!images.isEmpty()) {
        flashbackSlideshow.setImage(images.get(currentImageIndex));
      }

      // Show memory screen elements
      btnSend.setVisible(true);
      txtInput.setVisible(true);
      txtaChat.setVisible(true);
      dropUpArrow.setVisible(true);
      updateArrowToDropDown();
      nextButton.setVisible(false);

      // Show drag and drop elements for puzzle
      showDragAndDropElements();
    }
  }

  /** Shows the draggable event images and drop slots for the memory puzzle. */
  private void showDragAndDropElements() {
    event1Image.setVisible(true);
    event2Image.setVisible(true);
    event3Image.setVisible(true);
    dropSlot1.setVisible(true);
    dropSlot2.setVisible(true);
    dropSlot3.setVisible(true);
  }

  /** Sets up drag and drop functionality for event images. */
  private void setupDragAndDrop() {
    // Set up draggable event images
    setupDraggableImage(event1Image, "event1");
    setupDraggableImage(event2Image, "event2");
    setupDraggableImage(event3Image, "event3");

    // Set up drop targets
    setupDropTarget(dropSlot1, 0);
    setupDropTarget(dropSlot2, 1);
    setupDropTarget(dropSlot3, 2);
  }

  /** Sets up a draggable image with the specified event ID. */
  private void setupDraggableImage(ImageView imageView, String eventId) {
    // Change cursor to indicate draggable and add hover effect
    imageView.setOnMouseEntered(
        e -> {
          imageView.getScene().setCursor(javafx.scene.Cursor.OPEN_HAND);
          // Subtle scale up on hover
          ScaleTransition hoverScale = new ScaleTransition(Duration.millis(100), imageView);
          hoverScale.setToX(1.05);
          hoverScale.setToY(1.05);
          hoverScale.play();
        });

    imageView.setOnMouseExited(
        e -> {
          imageView.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
          // Scale back down
          ScaleTransition exitScale = new ScaleTransition(Duration.millis(100), imageView);
          exitScale.setToX(1.0);
          exitScale.setToY(1.0);
          exitScale.play();
        });

    imageView.setOnDragDetected(
        (MouseEvent event) -> {
          Dragboard dragboard = imageView.startDragAndDrop(TransferMode.MOVE);
          ClipboardContent content = new ClipboardContent();
          content.putString(eventId);
          dragboard.setContent(content);

          // Create drag image preview with better visual feedback
          javafx.scene.image.WritableImage dragImage = imageView.snapshot(null, null);
          dragboard.setDragView(dragImage, dragImage.getWidth() / 2, dragImage.getHeight() / 2);

          // Make original image semi-transparent during drag
          imageView.setOpacity(0.5);

          // Change cursor
          imageView.getScene().setCursor(javafx.scene.Cursor.CLOSED_HAND);

          event.consume();
        });

    // Reset opacity and cursor when drag ends
    imageView.setOnDragDone(
        (DragEvent event) -> {
          imageView.setOpacity(1.0);
          imageView.setScaleX(1.0); // Reset scale
          imageView.setScaleY(1.0);
          imageView.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
          event.consume();
        });
  }

  /** Sets up a drop target slot. */
  private void setupDropTarget(ImageView dropSlot, int slotIndex) {
    // Store original style for restoration
    String originalStyle = dropSlot.getStyle();

    dropSlot.setOnDragOver(
        (DragEvent event) -> {
          if (event.getGestureSource() != dropSlot && event.getDragboard().hasString()) {
            event.acceptTransferModes(TransferMode.MOVE);
            // Highlight drop zone with glowing effect
            dropSlot.setStyle(
                originalStyle + "; -fx-effect: dropshadow(gaussian, #00ff00, 10, 0.8, 0, 0);");
          }
          event.consume();
        });

    dropSlot.setOnDragExited(
        (DragEvent event) -> {
          // Remove highlight when drag leaves the area
          dropSlot.setStyle(originalStyle);
          event.consume();
        });

    dropSlot.setOnDragDropped(
        (DragEvent event) -> {
          // Remove highlight on drop
          dropSlot.setStyle(originalStyle);

          Dragboard dragboard = event.getDragboard();
          boolean success = false;

          if (dragboard.hasString()) {
            String eventId = dragboard.getString();

            // Clear any existing content in this slot
            if (slotContents[slotIndex] != null) {
              returnEventToOriginalPosition(slotContents[slotIndex]);
            }

            // Place the new event in this slot
            slotContents[slotIndex] = eventId;
            ImageView eventImage = getEventImageById(eventId);
            if (eventImage != null) {
              // Smooth animation to snap into place
              TranslateTransition snapTransition =
                  new TranslateTransition(Duration.millis(200), eventImage);
              snapTransition.setToX(dropSlot.getLayoutX() - eventImage.getLayoutX());
              snapTransition.setToY(dropSlot.getLayoutY() - eventImage.getLayoutY());
              snapTransition.setOnFinished(
                  e -> {
                    // Reset translate and update actual position
                    eventImage.setTranslateX(0);
                    eventImage.setTranslateY(0);
                    eventImage.setLayoutX(dropSlot.getLayoutX());
                    eventImage.setLayoutY(dropSlot.getLayoutY());

                    // Small scale pulse effect to show successful placement
                    ScaleTransition pulseScale =
                        new ScaleTransition(Duration.millis(150), eventImage);
                    pulseScale.setFromX(1.0);
                    pulseScale.setFromY(1.0);
                    pulseScale.setToX(1.1);
                    pulseScale.setToY(1.1);
                    pulseScale.setAutoReverse(true);
                    pulseScale.setCycleCount(2);
                    pulseScale.play();
                  });
              snapTransition.play();
            }

            success = true;
            displayEventPlacementMessage(eventId, slotIndex);
            checkWinCondition();
          }

          event.setDropCompleted(success);
          event.consume();
        });
  }

  /** Returns an event image to its original position. */
  private void returnEventToOriginalPosition(String eventId) {
    ImageView eventImage = getEventImageById(eventId);
    if (eventImage != null) {
      int index = getEventIndex(eventId);
      if (index >= 0) {
        // Smooth animation back to original position
        TranslateTransition returnTransition =
            new TranslateTransition(Duration.millis(300), eventImage);
        returnTransition.setToX(originalX[index] - eventImage.getLayoutX());
        returnTransition.setToY(originalY[index] - eventImage.getLayoutY());
        returnTransition.setOnFinished(
            e -> {
              // Reset translate and update actual position
              eventImage.setTranslateX(0);
              eventImage.setTranslateY(0);
              eventImage.setLayoutX(originalX[index]);
              eventImage.setLayoutY(originalY[index]);
            });
        returnTransition.play();
      }
    }
  }

  /** Gets the ImageView for a given event ID. */
  private ImageView getEventImageById(String eventId) {
    switch (eventId) {
      case "event1":
        return event1Image;
      case "event2":
        return event2Image;
      case "event3":
        return event3Image;
      default:
        return null;
    }
  }

  /** Gets the index for a given event ID. */
  private int getEventIndex(String eventId) {
    switch (eventId) {
      case "event1":
        return 0;
      case "event2":
        return 1;
      case "event3":
        return 2;
      default:
        return -1;
    }
  }

  /** Displays a message when an event is placed in a slot. */
  private void displayEventPlacementMessage(String eventId, int slotIndex) {
    String message = "";
    String contextMessage = "";

    // Only show message if the event is placed in the correct slot
    if ((eventId.equals("event1") && slotIndex == 0)
        || (eventId.equals("event2") && slotIndex == 1)
        || (eventId.equals("event3") && slotIndex == 2)) {

      switch (eventId) {
        case "event1":
          message = "Cassian made various statistic changes to the Project Starlight Logs.";
          contextMessage =
              "Player correctly placed the first timeline event: Cassian's Log Alteration. This"
                  + " represents the initial compromise where Cassian manipulated Project Starlight"
                  + " data and statistics, setting the chain of events in motion. Echo II"
                  + " recognizes this as the root cause of the mission crisis.";
          lastTimelineAction = "Logs Altered event placed correctly";
          break;
        case "event2":
          message = "Aegis often takes immediate, extreme action. Such as a counter-threat";
          contextMessage =
              "Player correctly placed the second timeline event: Aegis I's Counter-Threat"
                  + " Response. This represents Aegis I's immediate and extreme reaction to"
                  + " detecting the security breach. Echo II understands this as the escalation"
                  + " phase where Aegis I's aggressive protocols triggered further complications.";
          lastTimelineAction = "Counter-Threat event placed correctly";
          break;
        case "event3":
          message = "O. Vale, Mission Lead, received an overflow of messages.";
          contextMessage =
              "Player correctly placed the third timeline event: Orion Vale's Message Overflow"
                  + " Crisis. This represents the final cascade effect where communication systems"
                  + " became overwhelmed, paralyzing mission coordination. Echo II sees this as the"
                  + " culmination of the crisis sequence.";
          lastTimelineAction = "Outrage event placed correctly";
          break;
      }

      if (!message.isEmpty()) {
        // Echo speaks the message (assistant role)
        ChatMessage eventMessage = new ChatMessage("assistant", message);
        appendChatMessage(eventMessage);

        // Add detailed context for AI understanding
        addContextToChat("system", contextMessage);
      }
    } else {
      // Event placed in wrong slot - add context about the mistake
      String wrongPlacementContext =
          "Player placed "
              + getEventName(eventId)
              + " in slot "
              + (slotIndex + 1)
              + " but it belongs in slot "
              + getCorrectSlot(eventId)
              + ". Echo II is analyzing the incorrect placement and will provide guidance when"
              + " timeline validation occurs.";
      addContextToChat("system", wrongPlacementContext);
      lastTimelineAction =
          "Incorrect event placement - " + getEventName(eventId) + " in wrong position";
    }
  }

  // Helper method to get event name from ID
  private String getEventName(String eventId) {
    switch (eventId) {
      case "event1":
        return "Logs Altered";
      case "event2":
        return "Counter-Threat";
      case "event3":
        return "Outrage";
      default:
        return "Unknown Event";
    }
  }

  // Helper method to get correct slot number for an event (1-based)
  private int getCorrectSlot(String eventId) {
    switch (eventId) {
      case "event1":
        return 1;
      case "event2":
        return 2;
      case "event3":
        return 3;
      default:
        return 0;
    }
  }

  /** Checks if the events are in the correct order and handles win condition. */
  private void checkWinCondition() {
    // Check if all slots are filled
    if (slotContents[0] != null && slotContents[1] != null && slotContents[2] != null) {
      // Check if events are in correct order (event1, event2, event3)
      if ("event1".equals(slotContents[0])
          && "event2".equals(slotContents[1])
          && "event3".equals(slotContents[2])) {
        handleCorrectOrder();
      } else {
        handleIncorrectOrder();
      }
    }
  }

  /** Handles when events are placed in the correct order. */
  private void handleCorrectOrder() {
    System.out.println("Events are in the correct order!");

    // Add a 1 second delay before showing success message
    Platform.runLater(
        () -> {
          new Thread(
                  () -> {
                    try {
                      Thread.sleep(1000); // Wait 1 second
                      Platform.runLater(
                          () -> {
                            // Echo speaks the success message (assistant role)
                            ChatMessage successMessage =
                                new ChatMessage("assistant", "Timeline successfully loaded⏳✔️");
                            appendChatMessage(successMessage);

                            // Add context for AI understanding
                            addContextToChat(
                                "system",
                                "Player has successfully completed Echo II's timeline memory"
                                    + " puzzle. All events were placed in correct chronological"
                                    + " order: 1st - Logs Altered (Cassian made various statistic"
                                    + " changes to the Project Starlight Logs), 2nd -"
                                    + " Counter-Threat (Aegis often takes immediate, extreme"
                                    + " action. Such as a counter-threat), 3rd - Outrage (O. Vale,"
                                    + " Mission Lead, received an overflow of messages). This"
                                    + " represents the correct sequence of events during the"
                                    + " mission compromise.");
                            lastTimelineAction = "Timeline completed successfully";
                          });
                    } catch (InterruptedException e) {
                      e.printStackTrace();
                    }
                  })
              .start();
        });

    // Mark AI witness interaction as completed
    GameStateManager.getInstance().setInteractionFlag("EchoInt", true);
  }

  /** Handles when events are placed in an incorrect order. */
  private void handleIncorrectOrder() {
    System.out.println("Events are not in the correct order. Try again!");

    // Echo speaks the error message (assistant role)
    ChatMessage errorMessage =
        new ChatMessage("assistant", "ERROR: Incorrect timeline, try again. ❌");
    appendChatMessage(errorMessage);

    // Add context for AI understanding
    String currentOrder = getSlotContentsAsString();
    addContextToChat(
        "system",
        "Player attempted to complete the timeline but placed events in wrong order. Echo II"
            + " provided error feedback. Current placement: "
            + currentOrder
            + ". The correct chronological order should be: 1st - Logs Altered (Cassian's data"
            + " manipulation), 2nd - Counter-Threat (Aegis's extreme response), 3rd - Outrage (O."
            + " Vale receiving overflow of messages). Player needs to rearrange the events to match"
            + " this sequence.");
    lastTimelineAction = "Timeline attempt failed - incorrect order";

    // Optionally reset all events to original positions after a delay
    Platform.runLater(
        () -> {
          new Thread(
                  () -> {
                    try {
                      Thread.sleep(1000); // Wait 1 second
                      Platform.runLater(() -> resetAllEvents());
                    } catch (InterruptedException e) {
                      e.printStackTrace();
                    }
                  })
              .start();
        });
  }

  // Add context to chat history without displaying to user (for AI context)
  private void addContextToChat(String role, String contextMessage) {
    ChatMessage contextChatMessage = new ChatMessage(role, contextMessage);
    ChatHistory.addMessage(contextChatMessage, "system");
    // Note: This doesn't update the UI, only the chat history for AI context
    // This provides Echo II with comprehensive understanding of player timeline interactions
    // including puzzle state, event sequence analysis, and memory reconstruction progress
  }

  // Helper method to get current slot contents as a readable string
  private String getSlotContentsAsString() {
    StringBuilder order = new StringBuilder();
    for (int i = 0; i < slotContents.length; i++) {
      if (slotContents[i] != null) {
        String eventName = "";
        switch (slotContents[i]) {
          case "event1":
            eventName = "Logs Altered";
            break;
          case "event2":
            eventName = "Counter-Threat";
            break;
          case "event3":
            eventName = "Outrage";
            break;
        }
        order.append("Slot ").append(i + 1).append(": ").append(eventName);
        if (i < slotContents.length - 1 && slotContents[i + 1] != null) {
          order.append(", ");
        }
      } else {
        order.append("Slot ").append(i + 1).append(": empty");
        if (i < slotContents.length - 1) {
          order.append(", ");
        }
      }
    }
    return order.toString();
  }

  // Helper method to get current timeline puzzle status for AI context
  private String getTimelinePuzzleStatus() {
    int filledSlots = 0;
    for (String slot : slotContents) {
      if (slot != null) filledSlots++;
    }

    if (filledSlots == 0) {
      return "No events placed yet - puzzle awaiting player interaction";
    } else if (filledSlots < 3) {
      return filledSlots + " of 3 events placed - " + getSlotContentsAsString();
    } else {
      boolean isCorrect =
          "event1".equals(slotContents[0])
              && "event2".equals(slotContents[1])
              && "event3".equals(slotContents[2]);
      return "All 3 events placed - "
          + (isCorrect
              ? "CORRECT timeline sequence"
              : "INCORRECT sequence: " + getSlotContentsAsString());
    }
  }

  /** Override runGpt to add context about the last timeline action. */
  @Override
  protected ChatMessage runGpt(ChatMessage msg) throws ApiProxyException {
    // If there's a recent timeline action, add context to help the AI understand
    if (!lastTimelineAction.isEmpty()) {
      ChatMessage contextMsg =
          new ChatMessage(
              "system",
              "IMPORTANT CONTEXT: The user just interacted with Echo II's memory timeline puzzle"
                  + " system. Echo II is an AI witness with advanced memory reconstruction"
                  + " capabilities. The last action was: '"
                  + lastTimelineAction
                  + "'. "
                  + "Current timeline puzzle state: "
                  + getTimelinePuzzleStatus()
                  + ". If they're asking about the puzzle, events, timeline, mission sequence, or"
                  + " their recent actions, they are referring to this timeline memory interaction."
                  + " Echo II can provide detailed analysis of the Project Starlight mission events"
                  + " and their chronological significance.");
      chatCompletionRequest.addMessage(contextMsg);
    }

    // Call the parent runGpt method which now handles cleaning
    return super.runGpt(msg);
  }

  /** Resets all events to their original positions. */
  private void resetAllEvents() {
    for (int i = 0; i < slotContents.length; i++) {
      if (slotContents[i] != null) {
        returnEventToOriginalPosition(slotContents[i]);
        slotContents[i] = null;
      }
    }
  }

  // loading images for flashback
  private void loadImages(Runnable onLoaded) {
    new Thread(
            () -> {
              // loading images for animation flashback
              List<Image> loadedImages = new ArrayList<>();
              loadedImages.add(
                  new Image(getClass().getResourceAsStream("/images/flashbacks/ai/ai1F.png")));
              loadedImages.add(
                  new Image(getClass().getResourceAsStream("/images/flashbacks/ai/ai2F.png")));
              loadedImages.add(
                  new Image(getClass().getResourceAsStream("/images/flashbacks/ai/ai3F.png")));
              loadedImages.add(
                  new Image(getClass().getResourceAsStream("/images/memories/aiMem.png")));
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

  // Change to screen image
  @FXML
  protected void nextScene(ActionEvent event) throws ApiProxyException, IOException {
    currentImageIndex++;
    if (currentImageIndex < images.size()) {
      flashbackSlideshow.setImage(images.get(currentImageIndex));
    } else {
      flashbackSlideshow.setOnMouseClicked(null);
    }

    if (currentImageIndex == 3) {
      popupPane.setVisible(true);
      nextButton.setVisible(false);

      btnSend.setVisible(true);
      txtInput.setVisible(true);
      txtaChat.setVisible(true);

      dropUpArrow.setVisible(true); // Show drop up arrow when chat appears
      updateArrowToDropDown(); // Set initial arrow to drop down arrow

      // Show drag and drop elements when reaching the memory screen
      showDragAndDropElements();
    }
  }

  // Toggle chat visibility with drop-up/down animation
  @FXML
  private void toggleChatVisibility(ActionEvent event) {
    if (chatVisible) {
      // Drop down (hide)
      animateTranslate(txtaChat, 150.0);
      animateTranslate(txtInput, 150.0);
      animateTranslate(btnSend, 150.0);

      // Change to dropUpArrow shape and original position
      updateArrowToDropUp();
      chatVisible = false;
      btnSend.setVisible(false);
      txtInput.setVisible(false);
      txtaChat.setVisible(false);
    } else {
      // Drop up (show)
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

  // Arrow image
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
    dropUpArrow.setLayoutY(400.0);
    setArrowImage("/images/assets/chatDown.png");
  }

  // Update arrow to dropUp shape and original position
  private void updateArrowToDropUp() {
    dropUpArrow.setLayoutX(14.0);
    dropUpArrow.setLayoutY(540.0);
    setArrowImage("/images/assets/chatUp.png");
  }

  // Animate the vertical transition
  private void animateTranslate(javafx.scene.Node node, double toY) {
    TranslateTransition transition = new TranslateTransition(Duration.millis(300), node);
    transition.setToY(toY);
    transition.play();
  }

  /** Resets the controller to its initial state for game restart. */
  public void resetControllerState() {
    popupPane.setVisible(false);
    Platform.runLater(
        () -> {
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
            flashbackSlideshow.setVisible(true); // Ensure main slideshow is visible
          }

          // Hide the memory screen overlay
          if (aiFlashback != null) {
            aiFlashback.setVisible(false);
          }

          // Reset drag and drop puzzle elements
          resetPuzzleState();
        });
  }

  /** Resets the drag and drop puzzle to its initial state. */
  private void resetPuzzleState() {
    // Clear slot contents
    for (int i = 0; i < slotContents.length; i++) {
      slotContents[i] = null;
    }

    // Reset timeline context
    lastTimelineAction = "";

    // Hide all drag and drop elements
    if (event1Image != null) {
      event1Image.setVisible(false);
      event1Image.setOpacity(1.0);
      event1Image.setScaleX(1.0);
      event1Image.setScaleY(1.0);
      event1Image.setTranslateX(0);
      event1Image.setTranslateY(0);
    }
    if (event2Image != null) {
      event2Image.setVisible(false);
      event2Image.setOpacity(1.0);
      event2Image.setScaleX(1.0);
      event2Image.setScaleY(1.0);
      event2Image.setTranslateX(0);
      event2Image.setTranslateY(0);
    }
    if (event3Image != null) {
      event3Image.setVisible(false);
      event3Image.setOpacity(1.0);
      event3Image.setScaleX(1.0);
      event3Image.setScaleY(1.0);
      event3Image.setTranslateX(0);
      event3Image.setTranslateY(0);
    }

    // Hide drop slots
    if (dropSlot1 != null) {
      dropSlot1.setVisible(false);
    }
    if (dropSlot2 != null) {
      dropSlot2.setVisible(false);
    }
    if (dropSlot3 != null) {
      dropSlot3.setVisible(false);
    }

    // Reset event images to original scrambled positions (2-3-1 order)
    Platform.runLater(
        () -> {
          if (event1Image != null) {
            event1Image.setLayoutX(originalX[0]); // Position for event1 (550.0)
            event1Image.setLayoutY(originalY[0]);
          }
          if (event2Image != null) {
            event2Image.setLayoutX(originalX[1]); // Position for event2 (85.0)
            event2Image.setLayoutY(originalY[1]);
          }
          if (event3Image != null) {
            event3Image.setLayoutX(originalX[2]); // Position for event3 (320.0)
            event3Image.setLayoutY(originalY[2]);
          }
        });
  }
}
