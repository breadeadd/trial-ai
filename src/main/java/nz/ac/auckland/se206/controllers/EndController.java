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
import nz.ac.auckland.se206.CountdownTimer;
import nz.ac.auckland.se206.GameStateContext;

/**
 * Controller class for the room view. Handles user interactions within the room where the user can
 * chat with witnesses and defendant to gain a better understanding.
 */
public class EndController {

  // Static instance to allow access from countdownTimer
  public static EndController instance;
  private static GameStateContext context = new GameStateContext();

  @FXML private Button yesBtn;
  @FXML private Button noBtn;
  @FXML private Label questionTxt;
  @FXML private Label reasonText;
  @FXML private Label rationalTxt;
  @FXML private TextArea enterRationale;

  private MediaPlayer mediaPlayer;

  /**
   * Initializes the room view. If it's the first time initialization, it will provide instructions
   * via text-to-speech.
   */
  @FXML
  public void initialize() {
    // Set static instance for access from CountdownTimer
    instance = this;
    reasonText.setVisible(false);
  }

  // occurs when enter
  public void setVisible() {
    noBtn.setVisible(false);
    yesBtn.setVisible(false);
    enterRationale.setVisible(false);
    rationalTxt.setVisible(false);
    reasonText.setVisible(true);
  }

  public void setMessage(String caseType) {
    switch (caseType) {
      case "yes":
        questionTxt.setText("You chose YES. You are wrong.");
        break;
      case "no":
        questionTxt.setText("You chose NO. This is correct!");
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
          this.setMessage("yes");
        });
  }

  // human witness clicked

  @FXML
  private void noPressed(MouseEvent event) throws IOException {
    Platform.runLater(
        () -> {
          this.setMessage("no");
        });
  }

  @FXML
  private void guessMade(MouseEvent event) throws IOException {
    Platform.runLater(
        () -> {
          this.setVisible();
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
}
