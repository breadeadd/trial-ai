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
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
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

  /**
   * Initializes the chat view.
   *
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  @FXML
  public void initialize() throws ApiProxyException {
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
      backBtn.setDisable(false);

      btnSend.setVisible(true);
      txtInput.setVisible(true);
      txtaChat.setVisible(true);

      button1.setVisible(true);
      button2.setVisible(true);
      button3.setVisible(true);
      button4.setVisible(true);

      dropUpArrow.setVisible(true);
      updateArrowToDropDown();
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
    btn1img.setVisible(!btn1img.isVisible());

    if (allButtonsClicked()) {
      GameStateManager.getInstance().setInteractionFlag("AegisInt", true);
    }
  }

  @FXML
  private void button2Clicked(MouseEvent event) throws IOException {
    btn2img.setVisible(!btn2img.isVisible());

    if (allButtonsClicked()) {
      GameStateManager.getInstance().setInteractionFlag("AegisInt", true);
    }
  }

  @FXML
  private void button3Clicked(MouseEvent event) throws IOException {
    btn3img.setVisible(!btn3img.isVisible());

    if (allButtonsClicked()) {
      GameStateManager.getInstance().setInteractionFlag("AegisInt", true);
    }
  }

  @FXML
  private void button4Clicked(MouseEvent event) throws IOException {
    btn4img.setVisible(!btn4img.isVisible());

    if (allButtonsClicked()) {
      GameStateManager.getInstance().setInteractionFlag("AegisInt", true);
    }
  }

  // toggle chat visibility with drop up/down animation
  @FXML
  private void toggleChatVisibility(ActionEvent event) {
    if (chatVisible) {
      // Drop down (hide)
      animateTranslate(txtaChat, 150.0);
      animateTranslate(txtInput, 150.0);
      animateTranslate(btnSend, 150.0);

      // Change to dropUpArrow shape and position
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

      // Change to dropDownArrow shape
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
      dropUpArrow.setText("");
      dropUpArrow.setStyle("-fx-background-color: transparent;");
    } catch (Exception e) {
      System.err.println("Could not load arrow image: " + imagePath);
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
    Platform.runLater(
        () -> {
          currentImageIndex = 0;
          chatVisible = true;

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

          if (dropUpArrow != null) {
            dropUpArrow.setVisible(false);
            dropUpArrow.setLayoutX(14.0);
            dropUpArrow.setLayoutY(540.0);
          }

          if (nextButton != null) {
            nextButton.setVisible(true);
          }

          if (flashbackSlideshow != null && !images.isEmpty()) {
            flashbackSlideshow.setImage(images.get(0));
          }

          resetMemoryButtons();
        });
  }

  /** Resets all memory buttons to their initial state. */
  private void resetMemoryButtons() {
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
