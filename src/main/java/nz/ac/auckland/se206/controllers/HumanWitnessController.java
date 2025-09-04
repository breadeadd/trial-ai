package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.prompts.PromptEngineering;

/**
 * Controller class for the chat view. Handles user interactions and communication with the GPT
 * model via the API proxy.
 */
public class HumanWitnessController extends ChatController {
  @FXML private Rectangle screenBox;
  @FXML private ImageView flashback;

  /**
   * Initializes the chat view.
   *
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  @FXML
  public void initialize() throws ApiProxyException {
    // Any required initialization code can be placed here
    // loading.setProgress(-1);
    initChat();
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

  // Not first time
  public void runAfterFirst() {
    flashback.setImage(
        new Image(getClass().getResourceAsStream("/images/postFlashback/orion.jpeg")));
    screenBox.setVisible(false);
  }

  // Change to screen image
  @FXML
  private void screenClick(MouseEvent event) throws IOException {
    flashback.setImage(
        new Image(getClass().getResourceAsStream("/images/humanFlashback/screenInteract.jpg")));
  }
}
