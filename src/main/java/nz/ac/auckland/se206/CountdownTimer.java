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
  private static final IntegerProperty secondsRemaining = new SimpleIntegerProperty(5);
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
      // only run when 120 times out
      App.setRoot("answer");

      // TTS for last question
      try {
        playEndTtsAudio();
        secondsRemaining.set(60);
        guessed = true;
        start(); // Restart timer for final answer phase
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      // Final timeout - time ran out in answer scene
      if (secondsRemaining.get() == 0) {
        nz.ac.auckland.se206.controllers.EndController.instance.setMessage("timeout");
        nz.ac.auckland.se206.controllers.EndController.instance.setVisible();
        nz.ac.auckland.se206.controllers.EndController.instance.setRestartVisible();
        nz.ac.auckland.se206.controllers.EndController.instance.sentTimeoutRationale();
        // Don't restart timer after final timeout
        return;
      }
      start();
    }
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
