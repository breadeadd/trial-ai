package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest.Model;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.CountdownTimer;
import nz.ac.auckland.se206.GameStateContext;
import nz.ac.auckland.se206.prompts.PromptEngineering;

/**
 * Controller class for the room view. Handles user interactions within the room where the user can
 * chat with witnesses and defendant to gain a better understanding.
 */
public class EndController extends ChatController {

  // Static instance to allow access from countdownTimer
  public static EndController instance;
  private static GameStateContext context = new GameStateContext();

  @FXML private Button yesBtn;
  @FXML private Button noBtn;
  @FXML private Label questionTxt;
  @FXML private Label rationalTxt;
  @FXML private TextArea enterRationale;
  @FXML private Button guessBtn;

  private MediaPlayer mediaPlayer;

  private String verdictPlayer;

  /**
   * Initializes the room view. If it's the first time initialization, it will provide instructions
   * via text-to-speech.
   */
  @FXML
  public void initialize() {
    instance = this;
    txtaChat.setVisible(false);
    guessBtn.setDisable(true);
  }

  @Override
  protected String getSystemPrompt() {
    return PromptEngineering.getPrompt("verdict.txt");
  }

  @Override
  protected String getCharacterName() {
    return "";
  }

  @Override
  protected String getDisplayRole() {
    return "";
  }

  // occurs when enter
  public void setVisible() {
    noBtn.setVisible(false);
    yesBtn.setVisible(false);
    enterRationale.setVisible(false);
    rationalTxt.setVisible(false);
    guessBtn.setVisible(false);

    // set visible
    txtaChat.setVisible(true);
  }

  public void setMessage(String caseType) {
    switch (caseType) {
      case "GUILTY":
        questionTxt.setText("You chose GUILTY. You are correct!");
        break;
      case "NOT GUILTY":
        questionTxt.setText("You chose NOT GUILTY. This is correct!");
        break;
      case "timeout":
        questionTxt.setText("TIME OUT! YOU HAVE RUN OUT OF TIME.");
        break;
      default:
        questionTxt.setText("TIME OUT! YOU HAVE RUN OUT OF TIME.");
        break;
    }
  }

  // Call this method where you want to play the audio
  public void playEndTtsAudio() throws URISyntaxException {
    try {
      String audioPath = getClass().getResource("/audio/endTts.mp3").toURI().toString();
      Media media = new Media(audioPath);
      this.mediaPlayer = new MediaPlayer(media);
      mediaPlayer.setVolume(1.0);
      mediaPlayer.setOnReady(() -> mediaPlayer.play());
      mediaPlayer.setOnError(
          () -> {
            if (mediaPlayer.getError() != null) {
              mediaPlayer.getError().printStackTrace();
            }
          });
      mediaPlayer.play();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Handles the key pressed event.
   *
   * @param event the key event
   */
  @FXML
  public void onKeyPressed(KeyEvent event) {
    System.out.println("Key " + event.getCode() + " pressed");
  }

  /**
   * Handles the key released event.
   *
   * @param event the key event
   */
  @FXML
  public void onKeyReleased(KeyEvent event) {
    System.out.println("Key " + event.getCode() + " released");
  }

  /**
   * Handles mouse clicks on rectangles representing people in the room.
   *
   * @param event the mouse event triggered by clicking a rectangle
   * @throws IOException if there is an I/O error
   */

  // defendant clicked - switch scene

  @FXML
  private void yesPressed(MouseEvent event) throws IOException {
    Platform.runLater(
        () -> {
          yesBtn.setDisable(true);
          noBtn.setDisable(false);
          guessBtn.setDisable(false);
          verdictPlayer = "GUILTY";
        });
  }

  // human witness clicked

  @FXML
  private void noPressed(MouseEvent event) throws IOException {
    Platform.runLater(
        () -> {
          yesBtn.setDisable(false);
          noBtn.setDisable(true);
          guessBtn.setDisable(false);
          verdictPlayer = "NOT GUILTY";
        });
  }

  @FXML
  private void guessMade(MouseEvent event) throws IOException {
    Platform.runLater(
        () -> {
          this.setMessage(verdictPlayer);
          this.setVisible();

          // Get the rationale text and send to GPT without showing user input
          String rationaleText = enterRationale.getText().trim();

          // Putting together full message for GPT
          String fullMessage =
              String.format(
                  "CASE VERDICT ANALYSIS\n"
                      + "===================\n"
                      + "Player's Decision: %s\n"
                      + "Reasoning Provided: %s\n\n",
                  verdictPlayer, rationaleText);

          if (!rationaleText.isEmpty()) {
            // Send rationale to GPT in background thread - one-time response only
            new Thread(
                    () -> {
                      try {
                        getSingleGptResponse(fullMessage);
                      } catch (Exception e) {
                        e.printStackTrace();
                      }
                    })
                .start();

            // Clear the text area after sending
            enterRationale.clear();
          }

          CountdownTimer.stop();
        });
  }

  /**
   * Handles the guess button click event.
   *
   * @param event the action event triggered by clicking the guess button
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void handleGuessClick(ActionEvent event) throws IOException {
    context.handleGuessClick();
  }

  /**
   * Sends a single message to GPT and displays only the response. This is a one-time interaction,
   * not part of the ongoing chat conversation.
   *
   * @param userMessage the message to send to GPT
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  private void getSingleGptResponse(String userMessage) throws ApiProxyException {
    try {
      ApiProxyConfig config = ApiProxyConfig.readConfig();
      ChatCompletionRequest singleRequest =
          new ChatCompletionRequest(config)
              .setN(1)
              .setTemperature(0.2)
              .setTopP(0.5)
              .setModel(Model.GPT_4_1_MINI)
              .setMaxTokens(150);

      // Add system prompt and user message
      singleRequest.addMessage(new ChatMessage("system", getSystemPrompt()));
      singleRequest.addMessage(new ChatMessage("user", userMessage));

      // Execute and get response
      ChatCompletionResult result = singleRequest.execute();
      Choice choice = result.getChoices().iterator().next();
      ChatMessage response = choice.getChatMessage();

      // Display only the response in the chat area
      Platform.runLater(
          () -> {
            if (txtaChat != null) {
              txtaChat.appendText(response.getContent() + "\n\n");
            }
          });

    } catch (ApiProxyException e) {
      e.printStackTrace();
    }
  }
}
