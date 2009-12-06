package util.diff;

/**
 * Diff modifications.
 */
public class Modification {
  public static int TYPE_ADD     = 0;
  public static int TYPE_REMOVE  = 1;
  public static int TYPE_CHANGED = 2;

  private int type;
  private int startIndex;
  private int endIndex;

  public Modification(int type, int startIndex, int endIndex) {
    this.type = type;
    this.startIndex = startIndex;
    this.endIndex = endIndex;
  }

  public int getType() {
    return type;
  }

  public int getStartIndex() {
    return startIndex;
  }

  public int getEndIndex() {
    return endIndex;
  }
}
