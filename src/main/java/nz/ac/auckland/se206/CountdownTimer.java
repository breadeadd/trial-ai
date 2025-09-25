package nz.ac.auckland.se206;

import java.io.IOException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.util.Duration;

public class CountdownTimer {
  private static final int secondDuration = 1;
  private static Timeline countdownTimer;
  private static final IntegerProperty secondsRemaining = new SimpleIntegerProperty(300);
  private static boolean guessed = false;

  static {
    countdownTimer =
        new Timeline(
            new KeyFrame(
                Duration.seconds(secondDuration),
                (ActionEvent event) -> {
                  int currentSeconds = secondsRemaining.get();

                  if (currentSeconds > 0) {
                    secondsRemaining.set(currentSeconds - 1);
                  } else {
                    try {
                      guess();
                    } catch (IOException e) {
                      e.printStackTrace();
                    }
                  }
                }));

    countdownTimer.setCycleCount(Timeline.INDEFINITE);
  }

  public static void start() {
    countdownTimer.play();
  }

  public static void guess() throws IOException {
    countdownTimer.pause();
    if (!guessed) {
      // Check if player has talked to all characters when initial timer expires
      if (!nz.ac.auckland.se206.states.GameStateManager.getInstance().hasSpokenToAllCharacters()) {
        // Player loses immediately for not talking to all characters
        App.setRoot("answer");
        if (nz.ac.auckland.se206.controllers.EndController.instance != null) {
          nz.ac.auckland.se206.controllers.EndController.instance.setMessage("incomplete_interactions");
          nz.ac.auckland.se206.controllers.EndController.instance.setVisible();
          nz.ac.auckland.se206.controllers.EndController.instance.setRestartVisible();
          nz.ac.auckland.se206.controllers.EndController.instance.setIncompleteInteractionsRationale();
        }
        return; // Don't continue with normal timeout flow
      }
      
      // only run when 120 times out
      App.setRoot("answer");

      // TTS for last question
      try {
        playEndTtsAudio();
        secondsRemaining.set(60);
        guessed = true;
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      // Always show timeout message when timer reaches 0
      if (secondsRemaining.get() == 0) {
        nz.ac.auckland.se206.controllers.EndController.instance.setMessage("timeout");
        nz.ac.auckland.se206.controllers.EndController.instance.setVisible();
      }
    }
    start();
  }

  private static void playEndTtsAudio() {
    try {
      if (nz.ac.auckland.se206.controllers.EndController.instance != null) {
        nz.ac.auckland.se206.controllers.EndController.instance.playEndTtsAudio();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void stop() {
    countdownTimer.pause();
    secondsRemaining.set(0);
  }

  /** Resets the timer for a new game. */
  public static void reset() {
    countdownTimer.pause();
    secondsRemaining.set(300);
    guessed = false;
  }

  // Setting the seconds
  public static IntegerProperty secondsRemainingProperty() {
    return secondsRemaining;
  }
}
