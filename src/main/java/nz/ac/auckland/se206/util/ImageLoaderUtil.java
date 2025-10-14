package nz.ac.auckland.se206.util;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.image.Image;

/**
 * Utility class for loading character-specific flashback images and memory images in background
 * threads to avoid UI blocking.
 */
public class ImageLoaderUtil {

  /**
   * Loads flashback images for a specific character in a background thread.
   *
   * @param characterName the character name (defendant, human, ai)
   * @param images the list to populate with loaded images
   * @param onLoaded callback to execute when loading is complete (can be null)
   */
  public static void loadCharacterImages(
      String characterName, List<Image> images, Runnable onLoaded) {
    new Thread(
            () -> {
              List<Image> loadedImages = new ArrayList<>();

              // Load character-specific flashback images
              for (int i = 1; i <= 3; i++) {
                String path =
                    String.format(
                        "/images/flashbacks/%s/%s%dF.png", characterName, characterName, i);
                loadedImages.add(new Image(ImageLoaderUtil.class.getResourceAsStream(path)));
              }

              // Add memory image
              String memoryPath = getMemoryImagePath(characterName);
              if (memoryPath != null) {
                loadedImages.add(new Image(ImageLoaderUtil.class.getResourceAsStream(memoryPath)));
              }

              // Update image list on UI thread
              Platform.runLater(
                  () -> {
                    images.clear();
                    images.addAll(loadedImages);
                    if (onLoaded != null) {
                      onLoaded.run();
                    }
                  });
            })
        .start();
  }

  /**
   * Gets the memory image path for a specific character type. Each character has a unique memory
   * screen image that represents their interactive puzzle state. This method maps character names
   * to their corresponding memory image file paths in the resources directory.
   *
   * @param characterName the character name ("defendant", "human", or "ai")
   * @return the memory image path or null if character is not recognized
   */
  private static String getMemoryImagePath(String characterName) {
    // Convert character name to lowercase for case-insensitive matching
    switch (characterName.toLowerCase()) {
      case "defendant":
        // Return path to defendant's single memory screen image
        return "/images/memories/defendantMem.png";
      case "human":
        // Return path to human witness's first memory screen (locked phone)
        return "/images/memories/humanMem1.png";
      case "ai":
        // Return path to AI witness's memory screen with timeline puzzle
        return "/images/memories/aiMem.png";
      default:
        // Character not recognized - return null to indicate no memory image
        return null;
    }
  }

  /**
   * Loads flashback and memory images specifically for the human witness character. This method
   * handles the unique case where the human witness has two memory screen images (locked and
   * unlocked phone states) in addition to standard flashback images. The images are loaded in a
   * background thread to prevent UI blocking during startup.
   *
   * @param images the list to populate with loaded images in sequence
   * @param onLoaded callback to execute when loading is complete (can be null)
   */
  public static void loadHumanWitnessImages(List<Image> images, Runnable onLoaded) {
    new Thread(
            () -> {
              List<Image> loadedImages = new ArrayList<>();

              // Load human witness flashback images
              for (int i = 1; i <= 3; i++) {
                String path = String.format("/images/flashbacks/human/human%dF.png", i);
                loadedImages.add(new Image(ImageLoaderUtil.class.getResourceAsStream(path)));
              }

              // Add both memory images for human witness
              // Load first human memory image
              loadedImages.add(
                  new Image(
                      ImageLoaderUtil.class.getResourceAsStream("/images/memories/humanMem1.png")));
              // Load second human memory image
              loadedImages.add(
                  new Image(
                      ImageLoaderUtil.class.getResourceAsStream("/images/memories/humanMem2.png")));

              // Update image list on UI thread
              Platform.runLater(
                  () -> {
                    images.clear();
                    images.addAll(loadedImages);
                    if (onLoaded != null) {
                      onLoaded.run();
                    }
                  });
            })
        .start();
  }

  /**
   * Loads a single Image from a resource path using this class's classloader. Central helper to
   * reduce repeated new Image(...) callsites across controllers.
   *
   * @param resourcePath absolute resource path (e.g. "/images/characters/aegisIdle.png")
   * @return loaded Image
   */
  public static Image loadImage(String resourcePath) {
    return new Image(ImageLoaderUtil.class.getResourceAsStream(resourcePath));
  }
}
