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
import javafx.scene.shape.Rectangle;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.prompts.PromptEngineering;

/**
 * Controller class for the chat view. Handles user interactions and communication with the GPT
 * model via the API proxy.
 */
public class HumanWitnessController extends ChatController {
  private List<Image> images = new ArrayList<>();
  private int currentImageIndex = 0;

  @FXML private ImageView flashbackSlideshow;
  @FXML private Rectangle screenBox;
  @FXML private ImageView flashback;
  @FXML private Button nextButton;

  /**
   * Initializes the chat view.
   *
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  @FXML
  public void initialize() throws ApiProxyException {
    // Any required initialization code can be placed here
    // loading.setProgress(-1);
    loadImages(null);
    initChat();

    btnSend.setVisible(false);
    txtInput.setVisible(false);
    txtaChat.setVisible(false);
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
    flashback.setImage(new Image(getClass().getResourceAsStream("/images/memories/humanMem1.png")));
    screenBox.setVisible(false);
  }

  // loading images for flashback
  private void loadImages(Runnable onLoaded) {
    new Thread(
            () -> {
              // loading images for animation flashback
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
    }
  }
}
