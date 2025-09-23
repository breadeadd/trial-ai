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
import javafx.util.Duration;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.prompts.PromptEngineering;

/**
 * Controller class for the chat view. Handles user interactions and communication with the GPT
 * model via the API proxy.
 */
public class AiWitnessController extends ChatController {
  private List<Image> images = new ArrayList<>();
  private int currentImageIndex = 0;
  private boolean chatVisible = true; // Track chat visibility state

  @FXML private ImageView flashbackSlideshow;
  @FXML private ImageView aiFlashback;
  @FXML private Button nextButton;
  @FXML private Button backBtn;
  @FXML private Button dropUpArrow; // New button for toggling chat visibility

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
    dropUpArrow.setVisible(false); // Initially hidden
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

    // flashback ends and chat begins
    if (currentImageIndex == 3) {
      nextButton.setVisible(false);
      backBtn.setDisable(false);

      btnSend.setVisible(true);
      txtInput.setVisible(true);
      txtaChat.setVisible(true);

      dropUpArrow.setVisible(true); // Show drop up arrow when chat appears
      updateArrowToDropDown(); // Set arrow to drop down shape
    }
  }

  // Toggle chat visibility
  @FXML
  private void toggleChatVisibility(ActionEvent event) {
    if (chatVisible) {
      // Hide drop down
      animateTranslate(txtaChat, 150.0);
      animateTranslate(txtInput, 150.0);
      animateTranslate(btnSend, 150.0);

      // Change to dropUpArrow shape
      updateArrowToDropUp();
      chatVisible = false;
      btnSend.setVisible(false);
      txtInput.setVisible(false);
      txtaChat.setVisible(false);
    } else {
      // show drop up
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
      imageView.setFitWidth(40); // Adjust size as needed
      imageView.setFitHeight(40); // Adjust size as needed
      imageView.setPreserveRatio(true);
      dropUpArrow.setGraphic(imageView);
      dropUpArrow.setText(""); // Remove any text
      dropUpArrow.setStyle("-fx-background-color: transparent;");
    } catch (Exception e) {
      System.err.println("Could not load arrow image: " + imagePath);
      // Fallback to text if image fails
      dropUpArrow.setGraphic(null);
      dropUpArrow.setText("▼");
    }
  }

  // Updating arrow to dropDown shape
  private void updateArrowToDropDown() {
    dropUpArrow.setLayoutX(14.0);
    dropUpArrow.setLayoutY(400.0); // Adjust Y position above chatbox
    setArrowImage("/images/assets/chatDown.png");
  }

  // Update arrow to dropUp shape
  private void updateArrowToDropUp() {
    dropUpArrow.setLayoutX(14.0);
    dropUpArrow.setLayoutY(540.0);
    setArrowImage("/images/assets/chatUp.png");
  }

  // Animating the transition
  private void animateTranslate(javafx.scene.Node node, double toY) {
    TranslateTransition transition = new TranslateTransition(Duration.millis(300), node);
    transition.setToY(toY);
    transition.play();
  }
}
