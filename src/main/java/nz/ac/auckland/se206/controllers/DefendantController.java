package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
  // private Timeline animationTime; // No longer needed


  @FXML private ImageView flashbackSlideshow;

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
    // loading.setProgress(-1);
  }

  @FXML
  private void onKeyPressed(KeyEvent event) throws ApiProxyException, IOException {
    if (event.getCode() == KeyCode.ENTER) {
      onSendMessage(null); // Message gets send on enter key press
    }
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

  // loading images for flashback
  private void loadImages(Runnable onLoaded) {
    new Thread(
            () -> {
              // loading images for animation flashback
              List<Image> loadedImages = new ArrayList<>();
              loadedImages.add(
                  new Image(
                      getClass().getResourceAsStream("/images/defendantFlashback/defFlash1.jpg")));
              loadedImages.add(
                  new Image(
                      getClass().getResourceAsStream("/images/defendantFlashback/defFlash2.jpg")));
              loadedImages.add(
                  new Image(
                      getClass().getResourceAsStream("/images/defendantFlashback/defFlash3.jpg")));
              loadedImages.add(
                  new Image(
                      getClass().getResourceAsStream("/images/defendantFlashback/defFlash4.jpg")));
              loadedImages.add(
                  new Image(
                      getClass().getResourceAsStream("/images/defendantFlashback/defFlash5.jpg")));
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

  // run flashback slideshow
  public void startFlashbackSlideshow() {
    // Load images and start slideshow after loading
    if (images.isEmpty()) {
      loadImages(this::startFlashbackSlideshow);
      return;
    }

    currentImageIndex = 0;
    flashbackSlideshow.setImage(images.get(currentImageIndex));

    // Remove any previous event handler to avoid stacking
    flashbackSlideshow.setOnMouseClicked(null);

    flashbackSlideshow.setOnMouseClicked(event -> {
      currentImageIndex++;
      if (currentImageIndex < images.size()) {
        flashbackSlideshow.setImage(images.get(currentImageIndex));
      } else {
        // Optionally, remove the handler or do something when finished
        flashbackSlideshow.setOnMouseClicked(null);
      }
    });
  }

  public void runFlashback() {
    startFlashbackSlideshow();
  }

  // Not first time
  public void runAfterFirst() {
    flashbackSlideshow.setImage(
        new Image(getClass().getResourceAsStream("/images/postFlashback/aegis.jpeg")));
  }
}
