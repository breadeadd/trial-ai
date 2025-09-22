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
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
// import javafx.animation.KeyFrame;
// import javafx.animation.Timeline;
// import javafx.event.ActionEvent;
// import javafx.util.Duration;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.prompts.PromptEngineering;

/**
 * Controller class for the chat view. Handles user interactions and communication with the GPT
 * model via the API proxy.
 */
public class DefendantController extends ChatController {
  // slideshow variables
  private List<Image> images = new ArrayList<>();
  private int currentImageIndex = 0;

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

    if (currentImageIndex == 3) {
      nextButton.setVisible(false);

      btnSend.setVisible(true);
      txtInput.setVisible(true);
      txtaChat.setVisible(true);

      button1.setVisible(true);
      button2.setVisible(true);
      button3.setVisible(true);
      button4.setVisible(true);
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

  @FXML
  private void button1Clicked(MouseEvent event) throws IOException {
    btn1img.setVisible(!btn1img.isVisible());
  }

  @FXML
  private void button2Clicked(MouseEvent event) throws IOException {
    btn2img.setVisible(!btn2img.isVisible());
  }

  @FXML
  private void button3Clicked(MouseEvent event) throws IOException {
    btn3img.setVisible(!btn3img.isVisible());
  }

  @FXML
  private void button4Clicked(MouseEvent event) throws IOException {
    btn4img.setVisible(!btn4img.isVisible());
  }
}
