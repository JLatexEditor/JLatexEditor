package util.diff;

/**
 * Diff modifications.
 */
public class Modification {
  public static int TYPE_ADD     = 0;
  public static int TYPE_REMOVE  = 1;
  public static int TYPE_CHANGED = 2;

  private int type;
  private int sourceStartIndex;
  private int sourceLength;
  private int targetStartIndex;
  private int targetLength;

  public Modification(int type, int sourceStartIndex, int sourceLength, int targetStartIndex, int targetLength) {
    this.type = type;
    this.sourceStartIndex = sourceStartIndex;
    this.sourceLength = sourceLength;
    this.targetStartIndex = targetStartIndex;
    this.targetLength = targetLength;
  }

  public int getType() {
    return type;
  }

  public int getSourceStartIndex() {
    return sourceStartIndex;
  }

  public int getSourceLength() {
    return sourceLength;
  }

  public int getTargetStartIndex() {
    return targetStartIndex;
  }

  public int getTargetLength() {
    return targetLength;
  }

  public String toString() {
    return (type == TYPE_ADD ? "ADD " : (type == TYPE_REMOVE ? "REMOVE " : "CHANGED ")) +
            sourceStartIndex + ":" + sourceLength + " " + targetStartIndex + ":" + targetLength;
  }
}
