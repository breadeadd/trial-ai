package nz.ac.auckland.se206.controllers;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.prompts.PromptEngineering;

/**
 * Controller class for the chat view. Handles user interactions and communication with the GPT
 * model via the API proxy.
 */
public class AiWitnessController extends ChatController {
  @FXML private ImageView aiFlashback;

  /**
   * Initializes the chat view.
   *
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  @FXML
  public void initialize() throws ApiProxyException {
    initChat();
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

  // Not first time visiting
  public void runAfterFirst() {
    aiFlashback.setImage(new Image(getClass().getResourceAsStream("/images/memories/aiMem.png")));
  }
}
