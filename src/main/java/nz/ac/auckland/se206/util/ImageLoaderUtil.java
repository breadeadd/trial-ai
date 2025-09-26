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
                String path = String.format(
                    "/images/flashbacks/%s/%s%dF.png", 
                    characterName, 
                    characterName, 
                    i);
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
   * Gets the memory image path for a character.
   *
   * @param characterName the character name
   * @return the memory image path or null if not found
   */
  private static String getMemoryImagePath(String characterName) {
    switch (characterName.toLowerCase()) {
      case "defendant":
        return "/images/memories/defendantMem.png";
      case "human":
        return "/images/memories/humanMem1.png";
      case "ai":
        return "/images/memories/aiMem.png";
      default:
        return null;
    }
  }

  /**
   * Loads flashback images for human witness with both memory images.
   *
   * @param images the list to populate with loaded images
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
              loadedImages.add(new Image(
                  ImageLoaderUtil.class.getResourceAsStream("/images/memories/humanMem1.png")));
              // Load second human memory image
              loadedImages.add(new Image(
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
}