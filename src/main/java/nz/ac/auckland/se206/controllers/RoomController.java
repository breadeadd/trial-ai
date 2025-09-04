package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Rectangle;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.CountdownTimer;
import nz.ac.auckland.se206.states.GameStateManager;

/**
 * Controller class for the room view. Handles user interactions within the room where the user can
 * chat with witnesses and defendant to gain a better understanding.
 */
public class RoomController {
  private static boolean isFirstTimeInit = true;
  private boolean firstDefendant = false;
  private boolean firstHuman = false;
  private boolean firstAi = false;
  private MediaPlayer mediaPlayer; // Keep reference to prevent garbage collection

  @FXML private Rectangle rectCashier;
  @FXML private Rectangle rectPerson1;
  @FXML private Rectangle rectPerson2;
  @FXML private Rectangle rectPerson3;
  @FXML private Rectangle rectWaitress;
  @FXML private Button btnGuess;

  /**
   * Initializes the room view. If it's the first time initialization, it will provide instructions
   * via text-to-speech.
   */
  @FXML
  public void initialize() {
    if (isFirstTimeInit) {
      try {
        playOpenTtsAudio();
      } catch (URISyntaxException e) {
        e.printStackTrace();
      }
      isFirstTimeInit = false;
    }

    // always check if all characters have been spoken to
    Platform.runLater(() -> updateButtonState());
  }

  // Call this method where you want to play the audio
  private void playOpenTtsAudio() throws URISyntaxException {
    try {
      String audioPath = getClass().getResource("/audio/openTts.mp3").toURI().toString();
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
  private void defendantClicked(MouseEvent event) throws IOException {
    App.setRoot("defendantChat");

    DefendantController controller = (DefendantController) App.getController("defendantChat");
    controller.syncChatHistoryAsync();

    if (!firstDefendant) {
      controller.runFlashback();
      firstDefendant = true;
    } else {
      controller.runAfterFirst();
    }

    // GameStateManager.getInstance().setCharacterFlashbackWatched("Aegis I");
  }

  // human witness clicked

  @FXML
  private void humanWitnessClicked(MouseEvent event) throws IOException {
    App.setRoot("witnessChat");

    HumanWitnessController controller = (HumanWitnessController) App.getController("witnessChat");
    controller.syncChatHistoryAsync();

    if (!firstHuman) {
      firstHuman = true;
    } else {
      controller.runAfterFirst();
    }

    // GameStateManager.getInstance().setCharacterFlashbackWatched("Orion Vale");
  }

  // ai witness clicked

  @FXML
  private void aiWitnessClicked(MouseEvent event) throws IOException {
    App.setRoot("aiChat");

    AiWitnessController controller = (AiWitnessController) App.getController("aiChat");
    controller.syncChatHistoryAsync();

    if (!firstAi) {
      firstAi = true;
    } else {
      controller.runAfterFirst();
    }

    // GameStateManager.getInstance().setCharacterFlashbackWatched("Echo II");
  }

  /**
   * Handles the guess button click event.
   *
   * @param event the action event triggered by clicking the guess button
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void handleGuessClick(ActionEvent event) throws IOException {
    CountdownTimer.guess();
    App.setRoot("answer");
  }

  // In whatever controller has the button that should be locked
  public void updateButtonState() {
    System.out.println("Updating button state..."); // Debug
    GameStateManager.getInstance().printStatus(); // Debug
    boolean canProceed = GameStateManager.getInstance().hasSpokenToAllCharacters();
    btnGuess.setDisable(!canProceed);
  }

  // Call this method whenever you want to check/update the button state
  // For example, after each conversation or in initialize()
}
