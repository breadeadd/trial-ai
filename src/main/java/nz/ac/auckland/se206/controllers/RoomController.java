package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
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
  private boolean firstDefendant = false;
  private boolean firstHuman = false;
  private boolean firstAi = false;
  private MediaPlayer mediaPlayer; // Keep reference to prevent garbage collection

  // set images for hover
  private Image aegisIdle =
      new Image(getClass().getResourceAsStream("/images/characters/aegisIdle.png"));
  private Image aegisHover =
      new Image(getClass().getResourceAsStream("/images/characters/aegisHover.png"));

  private Image orionIdle =
      new Image(getClass().getResourceAsStream("/images/characters/orionIdle.png"));
  private Image orionHover =
      new Image(getClass().getResourceAsStream("/images/characters/orionHover.png"));

  private Image echoIdle =
      new Image(getClass().getResourceAsStream("/images/characters/echoIdle.png"));
  private Image echoHover =
      new Image(getClass().getResourceAsStream("/images/characters/echoHover.png"));

  @FXML private Rectangle humanWitness;
  @FXML private Rectangle aiWitness;
  @FXML private Rectangle defendant;

  // hover image animations
  @FXML private ImageView defImg;
  @FXML private ImageView humanImg;
  @FXML private ImageView aiImg;

  @FXML private Button btnGuess;
  @FXML private AnchorPane startPane;
  @FXML private Button startButton;
  @FXML private Label storyLabel;

  /**
   * Initializes the room view. If it's the first time initialization, it will provide instructions
   * via text-to-speech.
   */
  @FXML
  public void initialize() {
    // Set the story text
    storyLabel.setText(
        "AstroHelix's security system, Aegis I, detected executive Cassian Thorne"
            + " falsifying mission safety data although her motives aren’t clear. Aegis I responded"
            + " with the highest security measure. Chat with the characters to uncover the"
            + " details.");

    // always check if all characters have been spoken to
    Platform.runLater(() -> updateButtonState());
  }

  // Plays opening TTS audio with game instructions
  private void playOpenTtsAudio() throws URISyntaxException {
    try {
      // Load and configure opening audio
      String audioPath = getClass().getResource("/audio/openTts.mp3").toURI().toString();
      Media media = new Media(audioPath);
      this.mediaPlayer = new MediaPlayer(media);
      mediaPlayer.setVolume(1.0);
      // Auto-play when audio is ready
      mediaPlayer.setOnReady(() -> mediaPlayer.play());
      // Handle playback errors
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

  /**
   * Handles mouse click events on the defendant character to initiate conversation. Switches to the
   * defendant chat scene, synchronizes chat history, and manages the flashback sequence based on
   * whether this is the player's first visit.
   *
   * @param event the mouse event triggered by clicking the defendant
   * @throws IOException if there is an error loading the defendant chat scene
   */
  @FXML
  private void defendantClicked(MouseEvent event) throws IOException {
    // Navigate to defendant chat interface
    App.setRoot("defendantChat");

    // Get controller and sync chat history to maintain conversation continuity
    DefendantController controller = (DefendantController) App.getController("defendantChat");
    controller.syncChatHistoryAsync();

    // Show flashback sequence on first visit, memory screen on subsequent visits
    if (!firstDefendant) {
      controller.runFlashback(); // First visit - show flashback sequence
      firstDefendant = true;
    } else {
      controller.runAfterFirst(); // Return visit - skip to memory screen
    }
  }

  /**
   * Handles mouse click events on the human witness character to initiate conversation. Switches to
   * the human witness chat scene, synchronizes chat history, and tracks whether this is the first
   * visit to manage appropriate conversation flow.
   *
   * @param event the mouse event triggered by clicking the human witness
   * @throws IOException if there is an error loading the witness chat scene
   */
  @FXML
  private void humanWitnessClicked(MouseEvent event) throws IOException {
    App.setRoot("witnessChat");

    HumanWitnessController controller = (HumanWitnessController) App.getController("witnessChat");
    controller.syncChatHistoryAsync();

    // Track first visit and manage conversation flow appropriately
    if (!firstHuman) {
      // First visit - run full flashback sequence
      firstHuman = true;
    } else {
      // Return visit - skip to appropriate interaction state
      controller.runAfterFirst();
    }
  }

  /**
   * Handles mouse click events on the AI witness character to initiate conversation. Switches to
   * the AI witness chat scene, synchronizes chat history, and manages the flashback sequence and
   * timeline puzzle based on first-time visit status.
   *
   * @param event the mouse event triggered by clicking the AI witness
   * @throws IOException if there is an error loading the AI witness chat scene
   */
  @FXML
  private void aiWitnessClicked(MouseEvent event) throws IOException {
    // Navigate to AI witness chat interface
    App.setRoot("aiChat");

    // Get controller and sync chat history for continuity
    AiWitnessController controller = (AiWitnessController) App.getController("aiChat");
    controller.syncChatHistoryAsync();

    // Show flashback sequence on first visit, memory screen on subsequent visits
    if (!firstAi) {
      controller.runFlashback(); // First visit - show flashback sequence
      firstAi = true;
    } else {
      controller.runAfterFirst(); // Return visit - skip to memory screen
    }
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

  /**
   * Updates the verdict button state based on whether the player has spoken to all characters. This
   * method checks the game state and enables or disables the guess button accordingly, ensuring
   * players can only proceed to the verdict phase after gathering all evidence.
   */
  public void updateButtonState() {
    System.out.println("Updating button state..."); // Debug
    GameStateManager.getInstance().printStatus(); // Debug
    boolean canProceed = GameStateManager.getInstance().hasSpokenToAllCharacters();
    btnGuess.setDisable(!canProceed);
  }

  /** Resets the room controller state for game restart. */
  public void resetRoomState() {
    // Initialize all character visit tracking to allow flashback sequences
    firstDefendant = false;
    firstHuman = false;
    firstAi = false;

    // Refresh button accessibility state
    updateButtonState();
  }

  // setting hover animations
  @FXML
  private void defEntered() {
    defImg.setImage(aegisHover);
  }

  @FXML
  private void defExited() {
    defImg.setImage(aegisIdle);
  }

  @FXML
  private void humanEntered() {
    humanImg.setImage(orionHover);
  }

  @FXML
  private void humanExited() {
    humanImg.setImage(orionIdle);
  }

  @FXML
  private void aiEntered() {
    aiImg.setImage(echoHover);
  }

  @FXML
  private void aiExited() {
    aiImg.setImage(echoIdle);
  }

  /**
   * Updates the state of the verdict button based on whether all witnesses have been interviewed.
   * This method checks if the player has talked to all three witnesses and enables/disables the
   * verdict button accordingly. Should be called after each conversation or during initialization.
   */
  @FXML
  private void onStartGame(ActionEvent event) throws URISyntaxException {
    startPane.setVisible(false);
    playOpenTtsAudio();
    CountdownTimer.start();
  }
}
