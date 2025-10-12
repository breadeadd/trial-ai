package nz.ac.auckland.se206;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * This is the entry point of the JavaFX application. This class initializes and runs the JavaFX
 * application.
 */
public class App extends Application {

  // Bundle to hold both root and controller
  public static class SceneBundle {
    public final Parent root;
    public final Object controller;

    public SceneBundle(Parent root, Object controller) {
      this.root = root;
      this.controller = controller;
    }
  }

  private static Map<String, SceneBundle> preloadedBundles = new HashMap<>();
  private static StackPane stackPaneRoot;
  private static BorderPane rootLayout;
  private static Label timerLabel;
  private static Scene scene;

  /**
   * Gets the controller for a preloaded scene.
   *
   * @param fxml the name of the FXML file (without extension)
   * @return the controller instance, or null if not found
   */
  public static Object getController(String fxml) {
    SceneBundle bundle = preloadedBundles.get(fxml);
    return bundle == null ? null : bundle.controller;
  }

  /**
   * The main method that launches the JavaFX application.
   *
   * @param args the command line arguments
   */
  public static void main(final String[] args) {
    launch();
  }

  /**
   * Sets the root of the scene to the specified FXML file.
   *
   * @param fxml the name of the FXML file (without extension)
   * @throws IOException if the FXML file is not found
   */
  public static void setRoot(String fxml) throws IOException {
    SceneBundle bundle = preloadedBundles.get(fxml);
    if (bundle == null) {
      throw new IOException("Scene not preloaded: " + fxml);
    }
    rootLayout.setCenter(bundle.root);
    System.out.println("Switched to scene: " + fxml);
  }

  /**
   * Preloads an FXML scene in a background thread.
   *
   * @param fxml the name of the FXML file (without extension) to preload
   */
  public static void preloadSceneAsync(String fxml, CountDownLatch latch) {
    // Create background task for scene loading
    Task<Void> preloadTask =
        new Task<Void>() {
          @Override
          protected Void call() throws Exception {
            try {
              // Load FXML file and create scene bundle
              FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
              Parent loadedRoot = loader.load();
              Object loadedController = loader.getController();
              SceneBundle loadedBundle = new SceneBundle(loadedRoot, loadedController);
              // Store in preloaded cache on UI thread
              Platform.runLater(() -> preloadedBundles.put(fxml, loadedBundle));
            } catch (IOException e) {
              // Log preload failures
              System.err.println("Failed to preload scene: " + fxml);
              e.printStackTrace();
            } finally {
              // Signal task completion
              latch.countDown();
            }
            return null;
          }
        };
    // Start preload task in background thread
    new Thread(preloadTask).start();
  }

  /**
   * Opens the chat view and sets the profession in the chat controller.
   *
   * @param event the mouse event that triggered the method
   * @param profession the profession to set in the chat controller
   * @throws IOException if the FXML file is not found
   */
  public static void openChat(MouseEvent event, String bot) throws IOException {
    // Get current stage from event source
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    String fxml;
    // Determine which chat scene to load based on character type
    switch (bot) {
      case "defendant":
        fxml = "defendantChat";
        break;
      case "humanWitness":
        fxml = "witnessChat";
        break;
      case "aiWitness":
        fxml = "aiChat";
        break;
      default:
        throw new IllegalArgumentException("Unknown bot type: " + bot);
    }
    // Retrieve preloaded scene bundle
    SceneBundle bundle = preloadedBundles.get(fxml);
    if (bundle == null) {
      throw new IOException("Scene not preloaded: " + fxml);
    }

    // Switch to the requested chat scene
    scene = new Scene(bundle.root);
    stage.setScene(scene);
    stage.show();
  }

  /**
   * This method is invoked when the application starts. It loads and shows the "room" scene.
   *
   * @param stage the primary stage of the application
   * @throws IOException if the "src/main/resources/fxml/room.fxml" file is not found
   */
  @Override
  public void start(final Stage stage) throws IOException {
    rootLayout = new BorderPane();
    timerLabel = new Label();

    // Applying CSS styling for timer
    timerLabel.setStyle(
        "-fx-font-size: 24px;"
            + "-fx-padding: 8px;"
            + "-fx-background-color: #ffffff;"
            + "-fx-border-radius: 8px;"
            + "-fx-background-radius: 8px;"
            + "-fx-text-fill: #333333;"
            + "-fx-border-color: #4a90e2;"
            + "-fx-border-width: 2px;"
            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0.5, 0, 1);");

    new CountdownTimer();

    // Timers property
    timerLabel
        .textProperty()
        .bind(
            new javafx.beans.binding.StringBinding() {
              {
                bind(CountdownTimer.secondsRemainingProperty());
              }

              @Override
              protected String computeValue() {
                int totalSeconds = CountdownTimer.secondsRemainingProperty().get();
                int minutes = totalSeconds / 60;
                int seconds = totalSeconds % 60;
                return String.format("%d:%02d", minutes, seconds);
              }
            });

    // Layouts for timer and title
    stackPaneRoot = new StackPane(rootLayout, timerLabel);
    StackPane.setAlignment(timerLabel, Pos.TOP_RIGHT);
    scene = new Scene(stackPaneRoot, 800, 600);
    stage.setScene(scene);
    stage.setTitle("TrialAI");
    stage.show();

    // Warning style when timer is < 30 sec
    CountdownTimer.secondsRemainingProperty()
        .addListener(
            (obs, oldValue, newValue) -> {
              Platform.runLater(
                  () -> {
                    if (newValue.intValue() <= 30 && newValue.intValue() > 0) {
                      timerLabel.setStyle(
                          "-fx-font-size: 24px;"
                              + "-fx-padding: 8px;"
                              + "-fx-background-color: #ffcccc;"
                              + "-fx-border-radius: 8px;"
                              + "-fx-background-radius: 8px;"
                              + "-fx-text-fill: #e74c3c;"
                              + "-fx-border-color: #e74c3c;"
                              + "-fx-border-width: 2px;"
                              + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0.5, 0, 1);");
                    } else {
                      // Revert to normal style when above 30 seconds
                      timerLabel.setStyle(
                          "-fx-font-size: 24px;"
                              + "-fx-padding: 8px;"
                              + "-fx-background-color: #ffffff;"
                              + "-fx-border-radius: 8px;"
                              + "-fx-background-radius: 8px;"
                              + "-fx-text-fill: #333333;"
                              + "-fx-border-color: #4a90e2;"
                              + "-fx-border-width: 2px;"
                              + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0.5, 0, 1);");
                    }
                  });
            });

    // Background preloading
    final String[] scenesToPreload = {"room", "defendantChat", "witnessChat", "aiChat", "answer"};
    final CountDownLatch latch = new CountDownLatch(scenesToPreload.length);
    for (String fxml : scenesToPreload) {
      preloadSceneAsync(fxml, latch);
    }

    // Wait for preloading, then show room (timer starts via start button in RoomController)
    Task<Void> waitTask =
        new Task<Void>() {
          @Override
          protected Void call() throws Exception {
            latch.await();
            return null;
          }

          @Override
          protected void succeeded() {
            Platform.runLater(
                () -> {
                  try {
                    // Set current root to main page
                    setRoot("room");
                    SceneBundle roomBundle = preloadedBundles.get("room");
                    if (roomBundle != null && roomBundle.root != null) {
                      roomBundle.root.requestFocus();
                    }

                    // Output for debugging
                    System.out.println("All scenes preloaded and switched to room.");
                  } catch (IOException e) {
                    System.err.println("Failed to switch to room scene");
                    e.printStackTrace();
                  }
                });
          }
        };

    // Starting the task
    new Thread(waitTask).start();
  }
}
