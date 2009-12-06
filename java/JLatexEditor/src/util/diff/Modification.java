package util.diff;

import javax.xml.ws.soap.Addressing;

/**
 * Diff modifications.
 */
public class Modification {
  public static int TYPE_ADD     = 0;
  public static int TYPE_REMOVE  = 1;
  public static int TYPE_CHANGED = 2;

  private int type;
  private int sourceStartIndex;
  private int targetStartIndex;
  private int length;

  public Modification(int type, int sourceStartIndex, int targetStartIndex, int length) {
    this.type = type;
    this.sourceStartIndex = sourceStartIndex;
    this.targetStartIndex = targetStartIndex;
    this.length = length;
  }

  public int getType() {
    return type;
  }

  public int getSourceStartIndex() {
    return sourceStartIndex;
  }

  public int getTargetStartIndex() {
    return targetStartIndex;
  }

  public int getLength() {
    return length;
  }

  public String toString() {
    return (type == TYPE_ADD ? "ADD " : (type == TYPE_REMOVE ? "REMOVE " : "CHANGED ")) +
            sourceStartIndex + " " + targetStartIndex + " " + length;
  }
}
