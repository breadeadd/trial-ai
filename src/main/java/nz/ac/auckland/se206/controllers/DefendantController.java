package nz.ac.auckland.se206.controllers;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.prompts.PromptEngineering;

/**
 * Controller class for the chat view. Handles user interactions and communication with the GPT
 * model via the API proxy.
 */
public class DefendantController extends ChatController {
  // private ChatCompletionRequest chatCompletionRequest;

  // slideshow variables
  private List<Image> images = new ArrayList<>();
  private int currentImageIndex = 0;
  private Timeline animationTime;
  private final int slideshowDuration = 2;

  // @FXML private TextArea txtaChat;
  // @FXML private TextField txtInput;
  // @FXML private Button btnSend;
  @FXML private ImageView flashbackSlideshow;

  // @FXML private javafx.scene.control.ProgressIndicator loading;

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
    animationTime =
        new Timeline(
            new KeyFrame(
                Duration.seconds(slideshowDuration),
                (ActionEvent event) -> {
                  currentImageIndex = (currentImageIndex + 1);
                  flashbackSlideshow.setImage(images.get(currentImageIndex));
                }));
    animationTime.setCycleCount(images.size());
    animationTime.setAutoReverse(false);
    animationTime.play();
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
