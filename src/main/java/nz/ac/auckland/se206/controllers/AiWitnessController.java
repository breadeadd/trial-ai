package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.prompts.PromptEngineering;

/**
 * Controller class for the chat view. Handles user interactions and communication with the GPT
 * model via the API proxy.
 */
public class AiWitnessController extends ChatController {
  private List<Image> images = new ArrayList<>();
  private int currentImageIndex = 0;
  
  // Drag and drop variables
  private String[] slotContents = new String[3]; // Track what's in each slot
  private double[] originalX = new double[3]; // Store original positions
  private double[] originalY = new double[3];

  @FXML private ImageView flashbackSlideshow;
  @FXML private ImageView aiFlashback;
  @FXML private Button nextButton;
  
  // Event images (draggable)
  @FXML private ImageView event1Image;
  @FXML private ImageView event2Image;
  @FXML private ImageView event3Image;
  
  // Drop slots
  @FXML private ImageView dropSlot1;
  @FXML private ImageView dropSlot2;
  @FXML private ImageView dropSlot3;

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
    
    // Initialize drag and drop functionality
    setupDragAndDrop();
    
    // Store original positions of event images
    Platform.runLater(() -> {
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
    aiFlashback.setImage(new Image(getClass().getResourceAsStream("/images/memories/aiMem.png")));
    aiFlashback.setVisible(true);
    
    // Show drag and drop elements when on memory screen
    showDragAndDropElements();
  }
  
  /**
   * Shows the draggable event images and drop slots for the memory puzzle.
   */
  private void showDragAndDropElements() {
    event1Image.setVisible(true);
    event2Image.setVisible(true);
    event3Image.setVisible(true);
    dropSlot1.setVisible(true);
    dropSlot2.setVisible(true);
    dropSlot3.setVisible(true);
  }
  
  /**
   * Sets up drag and drop functionality for event images.
   */
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
  
  /**
   * Sets up a draggable image with the specified event ID.
   */
  private void setupDraggableImage(ImageView imageView, String eventId) {
    imageView.setOnDragDetected((MouseEvent event) -> {
      Dragboard dragboard = imageView.startDragAndDrop(TransferMode.MOVE);
      ClipboardContent content = new ClipboardContent();
      content.putString(eventId);
      dragboard.setContent(content);
      event.consume();
    });
  }
  
  /**
   * Sets up a drop target slot.
   */
  private void setupDropTarget(ImageView dropSlot, int slotIndex) {
    dropSlot.setOnDragOver((DragEvent event) -> {
      if (event.getGestureSource() != dropSlot && event.getDragboard().hasString()) {
        event.acceptTransferModes(TransferMode.MOVE);
      }
      event.consume();
    });
    
    dropSlot.setOnDragDropped((DragEvent event) -> {
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
          eventImage.setLayoutX(dropSlot.getLayoutX());
          eventImage.setLayoutY(dropSlot.getLayoutY());
        }
        
        success = true;
        checkWinCondition();
      }
      
      event.setDropCompleted(success);
      event.consume();
    });
  }
  
  /**
   * Returns an event image to its original position.
   */
  private void returnEventToOriginalPosition(String eventId) {
    ImageView eventImage = getEventImageById(eventId);
    if (eventImage != null) {
      int index = getEventIndex(eventId);
      if (index >= 0) {
        eventImage.setLayoutX(originalX[index]);
        eventImage.setLayoutY(originalY[index]);
      }
    }
  }
  
  /**
   * Gets the ImageView for a given event ID.
   */
  private ImageView getEventImageById(String eventId) {
    switch (eventId) {
      case "event1": return event1Image;
      case "event2": return event2Image;
      case "event3": return event3Image;
      default: return null;
    }
  }
  
  /**
   * Gets the index for a given event ID.
   */
  private int getEventIndex(String eventId) {
    switch (eventId) {
      case "event1": return 0;
      case "event2": return 1;
      case "event3": return 2;
      default: return -1;
    }
  }
  
  /**
   * Checks if the events are in the correct order and handles win condition.
   */
  private void checkWinCondition() {
    // Check if all slots are filled
    if (slotContents[0] != null && slotContents[1] != null && slotContents[2] != null) {
      // Check if events are in correct order (event1, event2, event3)
      if ("event1".equals(slotContents[0]) && 
          "event2".equals(slotContents[1]) && 
          "event3".equals(slotContents[2])) {
        handleCorrectOrder();
      } else {
        handleIncorrectOrder();
      }
    }
  }
  
  /**
   * Handles when events are placed in the correct order.
   * TODO: Implement the logic for what happens when the puzzle is solved correctly.
   */
  private void handleCorrectOrder() {
    System.out.println("Events are in the correct order!");
    // TODO: Add logic here for what happens when the events are in the right order
    // This could be:
    // - Show a success message
    // - Unlock additional chat functionality
    // - Trigger a special animation or scene
    // - Progress the story
  }
  
  /**
   * Handles when events are placed in an incorrect order.
   */
  private void handleIncorrectOrder() {
    System.out.println("Events are not in the correct order. Try again!");
    // Optionally reset all events to original positions after a delay
    Platform.runLater(() -> {
      new Thread(() -> {
        try {
          Thread.sleep(1000); // Wait 1 second
          Platform.runLater(() -> resetAllEvents());
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }).start();
    });
  }
  
  /**
   * Resets all events to their original positions.
   */
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
      nextButton.setVisible(false);

      btnSend.setVisible(true);
      txtInput.setVisible(true);
      txtaChat.setVisible(true);
      
      // Show drag and drop elements when reaching the memory screen
      showDragAndDropElements();
    }
  }
}
