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
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.prompts.PromptEngineering;

/**
 * Controller class for the chat view. Handles user interactions and communication with the GPT
 * model via the API proxy.
 */
public class HumanWitnessController extends ChatController {
  private List<Image> images = new ArrayList<>();
  private int currentImageIndex = 0;
  private boolean chatVisible = true; // Track chat visibility state

  @FXML private ImageView flashbackSlideshow;
  @FXML private Rectangle screenBox;
  @FXML private Button nextButton;
  @FXML private Slider unlockSlider;
  @FXML private Button dropUpArrow;
  @FXML private TextArea txtaChat;
  @FXML private TextField txtInput;
  @FXML private Button btnSend;

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
    unlockSlider.setVisible(false); // Slider is initially hidden
    dropUpArrow.setVisible(false); // Drop up arrow initially hidden
    // Set initial upward arrow shape
    dropUpArrow.setStyle("-fx-background-color: #ffffff; -fx-shape: 'M 0 15 L 15 0 L 30 15 Z';");
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
    currentImageIndex = 3; // Start at humanMem1.png
    flashbackSlideshow.setImage(images.get(currentImageIndex));
    unlockSlider.setVisible(true); // Show slider when on humanMem1.png
    unlockSlider.setDisable(false); // Enabling slider
    unlockSlider.setValue(0.0); // Resetting to starting pos
    dropUpArrow.setVisible(true); // Show arrow on humanMem1.png
    // Set to dropDownArrow shape
    updateArrowToDropDown();
    toggleChatVisibility(null);
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

    if (currentImageIndex == 3) { // When reaching humanMem1.png
      nextButton.setVisible(false);
      unlockSlider.setVisible(true);
      unlockSlider.setDisable(false);
      dropUpArrow.setVisible(true);
      updateArrowToDropDown();
      toggleChatVisibility(null);

      btnSend.setVisible(true);
      txtInput.setVisible(true);
      txtaChat.setVisible(true);
    } else if (currentImageIndex == 4) { // When reaching humanMem2.png
      unlockSlider.setVisible(false);
      dropUpArrow.setVisible(true);

      // Force chat to be hidden and set to dropUpArrow
      chatVisible = false;
      animateTranslate(txtaChat, 150.0);
      animateTranslate(txtInput, 150.0);
      animateTranslate(btnSend, 150.0);
      updateArrowToDropUp();
      btnSend.setVisible(false);
      txtInput.setVisible(false);
      txtaChat.setVisible(false);
    } else {
      dropUpArrow.setVisible(false); // Hide drop-up arrow on other screens
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
      flashbackSlideshow.setImage(images.get(currentImageIndex));
      unlockSlider.setDisable(true);
      unlockSlider.setVisible(false);
      dropUpArrow.setVisible(true);

      // Force chat to be hidden and set to dropUpArrow
      chatVisible = false;
      animateTranslate(txtaChat, 150.0);
      animateTranslate(txtInput, 150.0);
      animateTranslate(btnSend, 150.0);
      updateArrowToDropUp();
      btnSend.setVisible(false);
      txtInput.setVisible(false);
      txtaChat.setVisible(false);
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

  // Update arrow to dropDown shape and position above chatbox
  private void updateArrowToDropDown() {
    dropUpArrow.setLayoutX(14.0);
    dropUpArrow.setLayoutY(419.0);
    dropUpArrow.setStyle("-fx-background-color: #ffffff; -fx-shape: 'M 0 0 L 15 15 L 30 0 Z';");
  }

  // Update arrow to dropUp shape and original position
  private void updateArrowToDropUp() {
    dropUpArrow.setLayoutX(14.0);
    dropUpArrow.setLayoutY(540.0);
    dropUpArrow.setStyle("-fx-background-color: #ffffff; -fx-shape: 'M 0 15 L 15 0 L 30 15 Z';");
  }

  // Animate the vertical transition
  private void animateTranslate(javafx.scene.Node node, double toY) {
    TranslateTransition transition = new TranslateTransition(Duration.millis(300), node);
    transition.setToY(toY);
    transition.play();
  }
}
