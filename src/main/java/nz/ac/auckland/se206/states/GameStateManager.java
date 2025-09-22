package nz.ac.auckland.se206.states;

import java.util.HashMap;
import java.util.Map;

public class GameStateManager {
  private static GameStateManager instance;
  private Map<String, Boolean> charactersTalkedTo;
  private Map<String, Boolean> gameFlags;

  // Manage game state and character interactions
  private GameStateManager() {
    charactersTalkedTo = new HashMap<>();
    gameFlags = new HashMap<>();
    initializeCharacters();
    initializeInteractionFlags();
  }

  public static GameStateManager getInstance() {
    if (instance == null) {
      instance = new GameStateManager();
    }
    return instance;
  }

  private void initializeCharacters() {
    charactersTalkedTo.put("Aegis I", false);
    charactersTalkedTo.put("Echo II", false);
    charactersTalkedTo.put("Orion Vale", false);
  }

  // Mark a character as talked to
  public void setCharacterTalkedTo(String characterName) {
    charactersTalkedTo.put(characterName, true);
  }

  // Check if all characters have been talked to
  public boolean hasSpokenToAllCharacters() {
    return charactersTalkedTo.values().stream().allMatch(Boolean::booleanValue);
  }

  // Get specific character status
  public boolean hasSpokenTo(String characterName) {
    return charactersTalkedTo.getOrDefault(characterName, false);
  }

  // Interaction Flags
  private void initializeInteractionFlags() {
    gameFlags.put("AegisInt", false);
    gameFlags.put("EchoInt", false);
    gameFlags.put("OrionInt", false);
  }

  public void setInteractionFlag(String charInteract, boolean value) {
    gameFlags.put(charInteract, value);
  }

  public boolean getInteractionFlag(String charInteract) {
    return gameFlags.getOrDefault(charInteract, false);
  }

  /////////////////////////////////////////////////////////
  // IMPLEMENT WHEN BOTHERED: FLASHBACK STATE TRACKING

  // Set flashback visited true
  // public void setCharacterFlashbackWatched(String characterName) {
  //   gameFlags.put(characterName + "_flashback_watched", true);
  // }

  // public boolean hasWatchedFlashback(String characterName) {
  //   return gameFlags.getOrDefault(characterName + "_flashback_watched", false);
  // }

  // Generic flag system for future use
  public void setFlag(String flagName, boolean value) {
    gameFlags.put(flagName, value);
  }

  public boolean getFlag(String flagName) {
    return gameFlags.getOrDefault(flagName, false);
  }

  // // Reset for new game
  // public void reset() {
  //   charactersTalkedTo.replaceAll((k, v) -> false);
  //   gameFlags.clear();
  // }

  public void printStatus() {
    System.out.println("Characters talked to: " + charactersTalkedTo);
    System.out.println("All characters spoken to: " + hasSpokenToAllCharacters());
  }
}
