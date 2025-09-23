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
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.ChatHistory;
import nz.ac.auckland.se206.CountdownTimer;
import nz.ac.auckland.se206.GameStateContext;
import nz.ac.auckland.se206.prompts.PromptEngineering;
import nz.ac.auckland.se206.states.GameStateManager;

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
  @FXML private Button restartBtn;

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

          String fullMessage =
              String.format(
                  "CASE VERDICT ANALYSIS\n"
                      + "===================\n"
                      + "Player's Decision: %s\n"
                      + "Reasoning Provided: %s\n\n",
                  verdictPlayer, rationaleText);

          if (!rationaleText.isEmpty()) {
            // Show loading wheel
            if (loading != null) {
              loading.setVisible(true);
              loading.setProgress(-1); // Indeterminate progress
            }

            // Send rationale to GPT in background thread - one-time response only
            new Thread(
                    () -> {
                      try {
                        getSingleGptResponse(fullMessage);
                      } catch (Exception e) {
                        e.printStackTrace();
                      } finally {
                        // Hide loading wheel when done and show restart button
                        Platform.runLater(
                            () -> {
                              if (loading != null) {
                                loading.setVisible(false);
                              }
                              // Show restart button after GPT response
                              guessBtn.setVisible(false);
                              restartBtn.setVisible(true);
                            });
                      }
                    })
                .start();

            // Clear the text area after sending
            enterRationale.clear();
          } else {
            // If no rationale provided, show restart button immediately
            guessBtn.setVisible(false);
            restartBtn.setVisible(true);
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
   * Handles the restart game button click event.
   *
   * @param event the mouse event triggered by clicking the restart button
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void restartGame(MouseEvent event) throws IOException {
    Platform.runLater(() -> {
      try {
        // Reset all game state variables
        resetGameState();
        
        // Initialize all character chats immediately after restart
        initializeAllCharacterChats();
        
        // Switch back to room scene
        App.setRoot("room");
        
        // Reset room controller state and update button state
        RoomController roomController = (RoomController) App.getController("room");
        if (roomController != null) {
          roomController.resetRoomState();
        }
        
        // Restart the timer
        CountdownTimer.start();
        
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  /**
   * Initializes chat for all character controllers immediately after restart.
   */
  private void initializeAllCharacterChats() {
    // Small delay to ensure chat history has been cleared
    new Thread(() -> {
      try {
        Thread.sleep(500); // Short delay to ensure everything is reset
        Platform.runLater(() -> {
          // Reset and initialize DefendantController
          DefendantController defendantController = (DefendantController) App.getController("defendantChat");
          if (defendantController != null) {
            defendantController.resetControllerState();
            defendantController.initChat();
          }
          
          // Reset and initialize HumanWitnessController
          HumanWitnessController humanController = (HumanWitnessController) App.getController("witnessChat");
          if (humanController != null) {
            humanController.resetControllerState();
            humanController.initChat();
          }
          
          // Reset and initialize AiWitnessController
          AiWitnessController aiController = (AiWitnessController) App.getController("aiChat");
          if (aiController != null) {
            aiController.resetControllerState();
            aiController.initChat();
          }
        });
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }).start();
  }

  /**
   * Resets all game state variables to initial values for a new game.
   */
  private void resetGameState() {
    System.out.println("Starting game state reset...");
    
    // Reset EndController state
    verdictPlayer = null;
    
    // Reset button visibility and states
    yesBtn.setVisible(true);
    yesBtn.setDisable(false);
    noBtn.setVisible(true);
    noBtn.setDisable(false);
    questionTxt.setText("IS AEGIS I GUILTY OF MAKING AN UNETHICAL DECISION?");
    rationalTxt.setVisible(true);
    enterRationale.setVisible(true);
    enterRationale.clear();
    guessBtn.setVisible(true);
    guessBtn.setDisable(true);
    restartBtn.setVisible(false);
    txtaChat.setVisible(false);
    txtaChat.clear();
    
    // Reset timer to initial 5 minutes and reset guessed flag
    CountdownTimer.reset();
    System.out.println("Timer reset to 5 minutes");
    
    // Clear chat history using reflection since there's no public clear method
    clearChatHistory();
    System.out.println("Chat history cleared");
    
    // Clear all chat controller UIs
    clearAllChatControllerUIs();
    System.out.println("All chat UIs cleared");
    
    // Reset chat completion requests
    resetChatCompletionRequests();
    System.out.println("Chat completion requests reset");
    
    // Reset GameStateManager
    resetGameStateManager();
    System.out.println("GameStateManager reset");
    
    // Reset RoomController first-time flags
    resetRoomControllerFlags();
    System.out.println("RoomController flags reset - flashbacks will trigger again");
    
    // Reset flashback states in all character controllers
    resetFlashbackStates();
    System.out.println("Flashback states reset in all character controllers");
    
    // Create new game context for new game
    context = new GameStateContext();
    System.out.println("New GameStateContext created");
    
    System.out.println("Game state reset complete! Ready for fresh game with flashbacks.");
  }

  /**
   * Clears the chat history using reflection.
   */
  private void clearChatHistory() {
    try {
      java.lang.reflect.Field historyField = ChatHistory.class.getDeclaredField("history");
      historyField.setAccessible(true);
      java.util.List<?> history = (java.util.List<?>) historyField.get(null);
      history.clear();
    } catch (Exception e) {
      System.err.println("Warning: Could not clear chat history: " + e.getMessage());
    }
  }

  /**
   * Clears all chat controller UI text areas to remove displayed messages.
   */
  private void clearAllChatControllerUIs() {
    // Clear defendant chat UI
    try {
      DefendantController defendantController = (DefendantController) App.getController("defendantChat");
      if (defendantController != null) {
        clearChatControllerUI(defendantController);
      }
    } catch (Exception e) {
      System.err.println("Warning: Could not clear defendant chat UI: " + e.getMessage());
    }

    // Clear human witness chat UI
    try {
      HumanWitnessController humanController = (HumanWitnessController) App.getController("witnessChat");
      if (humanController != null) {
        clearChatControllerUI(humanController);
      }
    } catch (Exception e) {
      System.err.println("Warning: Could not clear human witness chat UI: " + e.getMessage());
    }

    // Clear AI witness chat UI
    try {
      AiWitnessController aiController = (AiWitnessController) App.getController("aiChat");
      if (aiController != null) {
        clearChatControllerUI(aiController);
      }
    } catch (Exception e) {
      System.err.println("Warning: Could not clear AI witness chat UI: " + e.getMessage());
    }
  }

  /**
   * Clears a specific chat controller's UI using reflection.
   */
  private void clearChatControllerUI(ChatController controller) {
    try {
      java.lang.reflect.Field txtaChatField = ChatController.class.getDeclaredField("txtaChat");
      txtaChatField.setAccessible(true);
      javafx.scene.control.TextArea txtaChat = (javafx.scene.control.TextArea) txtaChatField.get(controller);
      if (txtaChat != null) {
        Platform.runLater(() -> txtaChat.clear());
      }
    } catch (Exception e) {
      System.err.println("Warning: Could not clear chat controller UI: " + e.getMessage());
    }
  }

  /**
   * Resets the ChatCompletionRequest objects in all chat controllers to start fresh.
   */
  private void resetChatCompletionRequests() {
    // Reset defendant chat request
    try {
      DefendantController defendantController = (DefendantController) App.getController("defendantChat");
      if (defendantController != null) {
        resetChatCompletionRequest(defendantController);
      }
    } catch (Exception e) {
      System.err.println("Warning: Could not reset defendant chat request: " + e.getMessage());
    }

    // Reset human witness chat request
    try {
      HumanWitnessController humanController = (HumanWitnessController) App.getController("witnessChat");
      if (humanController != null) {
        resetChatCompletionRequest(humanController);
      }
    } catch (Exception e) {
      System.err.println("Warning: Could not reset human witness chat request: " + e.getMessage());
    }

    // Reset AI witness chat request
    try {
      AiWitnessController aiController = (AiWitnessController) App.getController("aiChat");
      if (aiController != null) {
        resetChatCompletionRequest(aiController);
      }
    } catch (Exception e) {
      System.err.println("Warning: Could not reset AI witness chat request: " + e.getMessage());
    }
  }

  /**
   * Resets a specific chat controller's ChatCompletionRequest using reflection.
   */
  private void resetChatCompletionRequest(ChatController controller) {
    try {
      java.lang.reflect.Field requestField = ChatController.class.getDeclaredField("chatCompletionRequest");
      requestField.setAccessible(true);
      requestField.set(controller, null);
    } catch (Exception e) {
      System.err.println("Warning: Could not reset chat completion request: " + e.getMessage());
    }
  }

  /**
   * Resets the GameStateManager by reinitializing character states.
   */
  private void resetGameStateManager() {
    try {
      java.lang.reflect.Field charactersField = GameStateManager.class.getDeclaredField("charactersTalkedTo");
      charactersField.setAccessible(true);
      @SuppressWarnings("unchecked")
      java.util.Map<String, Boolean> characters = (java.util.Map<String, Boolean>) charactersField.get(GameStateManager.getInstance());
      characters.replaceAll((k, v) -> false);
      
      java.lang.reflect.Field flagsField = GameStateManager.class.getDeclaredField("gameFlags");
      flagsField.setAccessible(true);
      @SuppressWarnings("unchecked")
      java.util.Map<String, Boolean> flags = (java.util.Map<String, Boolean>) flagsField.get(GameStateManager.getInstance());
      flags.clear();
    } catch (Exception e) {
      System.err.println("Warning: Could not reset GameStateManager: " + e.getMessage());
    }
  }

  /**
   * Resets the first-time interaction flags in RoomController.
   */
  private void resetRoomControllerFlags() {
    try {
      RoomController roomController = (RoomController) App.getController("room");
      if (roomController != null) {
        // Use reflection to reset private boolean fields
        java.lang.reflect.Field firstDefendantField = RoomController.class.getDeclaredField("firstDefendant");
        firstDefendantField.setAccessible(true);
        firstDefendantField.set(roomController, false);
        
        java.lang.reflect.Field firstHumanField = RoomController.class.getDeclaredField("firstHuman");
        firstHumanField.setAccessible(true);
        firstHumanField.set(roomController, false);
        
        java.lang.reflect.Field firstAiField = RoomController.class.getDeclaredField("firstAi");
        firstAiField.setAccessible(true);
        firstAiField.set(roomController, false);
        
        // Reset the static isFirstTimeInit flag so opening audio plays again
        java.lang.reflect.Field isFirstTimeInitField = RoomController.class.getDeclaredField("isFirstTimeInit");
        isFirstTimeInitField.setAccessible(true);
        isFirstTimeInitField.set(null, true);
        
        System.out.println("Successfully reset RoomController flags for flashback restart");
      }
    } catch (Exception e) {
      System.err.println("Warning: Could not reset RoomController flags: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Resets the flashback states in all character controllers.
   */
  private void resetFlashbackStates() {
    // Reset DefendantController flashback state
    try {
      DefendantController defendantController = (DefendantController) App.getController("defendantChat");
      if (defendantController != null) {
        java.lang.reflect.Field currentImageIndexField = DefendantController.class.getDeclaredField("currentImageIndex");
        currentImageIndexField.setAccessible(true);
        currentImageIndexField.set(defendantController, 0);
        
        // Reset UI elements to initial state
        resetDefendantUI(defendantController);
        
        System.out.println("Reset DefendantController currentImageIndex to 0 and UI elements");
      }
    } catch (Exception e) {
      System.err.println("Warning: Could not reset DefendantController flashback state: " + e.getMessage());
    }

    // Reset HumanWitnessController flashback state  
    try {
      HumanWitnessController humanController = (HumanWitnessController) App.getController("witnessChat");
      if (humanController != null) {
        java.lang.reflect.Field currentImageIndexField = HumanWitnessController.class.getDeclaredField("currentImageIndex");
        currentImageIndexField.setAccessible(true);
        currentImageIndexField.set(humanController, 0);
        
        // Also reset chatVisible state for HumanWitnessController
        java.lang.reflect.Field chatVisibleField = HumanWitnessController.class.getDeclaredField("chatVisible");
        chatVisibleField.setAccessible(true);
        chatVisibleField.set(humanController, true);
        
        // Reset UI elements to initial state
        resetHumanWitnessUI(humanController);
        
        System.out.println("Reset HumanWitnessController currentImageIndex to 0, chatVisible to true, and UI elements");
      }
    } catch (Exception e) {
      System.err.println("Warning: Could not reset HumanWitnessController flashback state: " + e.getMessage());
    }

    // Reset AiWitnessController flashback state
    try {
      AiWitnessController aiController = (AiWitnessController) App.getController("aiChat");
      if (aiController != null) {
        java.lang.reflect.Field currentImageIndexField = AiWitnessController.class.getDeclaredField("currentImageIndex");
        currentImageIndexField.setAccessible(true);
        currentImageIndexField.set(aiController, 0);
        
        // Reset UI elements to initial state
        resetAiWitnessUI(aiController);
        
        System.out.println("Reset AiWitnessController currentImageIndex to 0 and UI elements");
      }
    } catch (Exception e) {
      System.err.println("Warning: Could not reset AiWitnessController flashback state: " + e.getMessage());
    }
  }

  /**
   * Resets DefendantController UI elements to initial state.
   */
  private void resetDefendantUI(DefendantController controller) {
    try {
      // Reset button visibility
      java.lang.reflect.Field btnSendField = ChatController.class.getDeclaredField("btnSend");
      btnSendField.setAccessible(true);
      javafx.scene.control.Button btnSend = (javafx.scene.control.Button) btnSendField.get(controller);
      if (btnSend != null) btnSend.setVisible(false);
      
      java.lang.reflect.Field txtInputField = ChatController.class.getDeclaredField("txtInput");
      txtInputField.setAccessible(true);
      javafx.scene.control.TextField txtInput = (javafx.scene.control.TextField) txtInputField.get(controller);
      if (txtInput != null) txtInput.setVisible(false);
      
      java.lang.reflect.Field txtaChatField = ChatController.class.getDeclaredField("txtaChat");
      txtaChatField.setAccessible(true);
      javafx.scene.control.TextArea txtaChat = (javafx.scene.control.TextArea) txtaChatField.get(controller);
      if (txtaChat != null) txtaChat.setVisible(false);
      
      // Reset nextButton visibility
      java.lang.reflect.Field nextButtonField = DefendantController.class.getDeclaredField("nextButton");
      nextButtonField.setAccessible(true);
      javafx.scene.control.Button nextButton = (javafx.scene.control.Button) nextButtonField.get(controller);
      if (nextButton != null) nextButton.setVisible(true);
      
      System.out.println("Reset DefendantController UI elements");
    } catch (Exception e) {
      System.err.println("Warning: Could not reset DefendantController UI: " + e.getMessage());
    }
  }

  /**
   * Resets HumanWitnessController UI elements to initial state.
   */
  private void resetHumanWitnessUI(HumanWitnessController controller) {
    try {
      // Reset button visibility
      java.lang.reflect.Field btnSendField = ChatController.class.getDeclaredField("btnSend");
      btnSendField.setAccessible(true);
      javafx.scene.control.Button btnSend = (javafx.scene.control.Button) btnSendField.get(controller);
      if (btnSend != null) btnSend.setVisible(false);
      
      java.lang.reflect.Field txtInputField = ChatController.class.getDeclaredField("txtInput");
      txtInputField.setAccessible(true);
      javafx.scene.control.TextField txtInput = (javafx.scene.control.TextField) txtInputField.get(controller);
      if (txtInput != null) txtInput.setVisible(false);
      
      java.lang.reflect.Field txtaChatField = ChatController.class.getDeclaredField("txtaChat");
      txtaChatField.setAccessible(true);
      javafx.scene.control.TextArea txtaChat = (javafx.scene.control.TextArea) txtaChatField.get(controller);
      if (txtaChat != null) txtaChat.setVisible(false);
      
      // Reset nextButton visibility
      java.lang.reflect.Field nextButtonField = HumanWitnessController.class.getDeclaredField("nextButton");
      nextButtonField.setAccessible(true);
      javafx.scene.control.Button nextButton = (javafx.scene.control.Button) nextButtonField.get(controller);
      if (nextButton != null) nextButton.setVisible(true);
      
      // Reset HumanWitness-specific UI elements
      java.lang.reflect.Field unlockSliderField = HumanWitnessController.class.getDeclaredField("unlockSlider");
      unlockSliderField.setAccessible(true);
      javafx.scene.control.Slider unlockSlider = (javafx.scene.control.Slider) unlockSliderField.get(controller);
      if (unlockSlider != null) {
        unlockSlider.setVisible(false);
        unlockSlider.setValue(0.0);
        unlockSlider.setDisable(false);
      }
      
      java.lang.reflect.Field dropUpArrowField = HumanWitnessController.class.getDeclaredField("dropUpArrow");
      dropUpArrowField.setAccessible(true);
      javafx.scene.control.Button dropUpArrow = (javafx.scene.control.Button) dropUpArrowField.get(controller);
      if (dropUpArrow != null) {
        dropUpArrow.setVisible(false);
        dropUpArrow.setStyle("-fx-background-color: #ffffff; -fx-shape: 'M 0 15 L 15 0 L 30 15 Z';");
      }
      
      System.out.println("Reset HumanWitnessController UI elements");
    } catch (Exception e) {
      System.err.println("Warning: Could not reset HumanWitnessController UI: " + e.getMessage());
    }
  }

  /**
   * Resets AiWitnessController UI elements to initial state.
   */
  private void resetAiWitnessUI(AiWitnessController controller) {
    try {
      // Reset button visibility
      java.lang.reflect.Field btnSendField = ChatController.class.getDeclaredField("btnSend");
      btnSendField.setAccessible(true);
      javafx.scene.control.Button btnSend = (javafx.scene.control.Button) btnSendField.get(controller);
      if (btnSend != null) btnSend.setVisible(false);
      
      java.lang.reflect.Field txtInputField = ChatController.class.getDeclaredField("txtInput");
      txtInputField.setAccessible(true);
      javafx.scene.control.TextField txtInput = (javafx.scene.control.TextField) txtInputField.get(controller);
      if (txtInput != null) txtInput.setVisible(false);
      
      java.lang.reflect.Field txtaChatField = ChatController.class.getDeclaredField("txtaChat");
      txtaChatField.setAccessible(true);
      javafx.scene.control.TextArea txtaChat = (javafx.scene.control.TextArea) txtaChatField.get(controller);
      if (txtaChat != null) txtaChat.setVisible(false);
      
      // Reset nextButton visibility
      java.lang.reflect.Field nextButtonField = AiWitnessController.class.getDeclaredField("nextButton");
      nextButtonField.setAccessible(true);
      javafx.scene.control.Button nextButton = (javafx.scene.control.Button) nextButtonField.get(controller);
      if (nextButton != null) nextButton.setVisible(true);
      
      System.out.println("Reset AiWitnessController UI elements");
    } catch (Exception e) {
      System.err.println("Warning: Could not reset AiWitnessController UI: " + e.getMessage());
    }
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
              .setMaxTokens(300);

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
