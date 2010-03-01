package util.diff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Diff modifications.
 */
public class Modification<T> {
  public static final int TYPE_ADD = 0;
  public static final int TYPE_REMOVE = 1;
  public static final int TYPE_CHANGED = 2;

  private int type;
  private int sourceStartIndex;
  private List<T> sourceLines;

  private int targetStartIndex;
  private List<T> targetLines;

  public Modification(int type, int sourceStartIndex, T[] sourceLines, int targetStartIndex, T[] targetLines) {
    this(type, sourceStartIndex, Arrays.asList(sourceLines), targetStartIndex, Arrays.asList(targetLines));
  }

  public Modification(int type, int sourceStartIndex, List<T> sourceLines, int targetStartIndex, List<T> targetLines) {
    this.type = type;
    this.sourceStartIndex = sourceStartIndex;
    this.sourceLines = sourceLines;
    this.targetStartIndex = targetStartIndex;
    this.targetLines = targetLines;
  }

  public int getType() {
    return type;
  }

  public int getSourceStartIndex() {
    return sourceStartIndex;
  }

  public int getSourceLength() {
    return sourceLines.size();
  }

  public List<T> getSourceLines() {
    return sourceLines;
  }

  public int getTargetStartIndex() {
    return targetStartIndex;
  }

  public int getTargetLength() {
    return targetLines.size();
  }

  public List<T> getTargetLines() {
    return targetLines;
  }

  /**
   * Applies the list of modifications to the document.
   *
   * @param document
   * @param modifications sorted, non-overlapping modifications
   * @return modified document
   */
  public static <T> List<T> apply(List<T> document, List<Modification<T>> modifications) {
    ArrayList<T> modified = new ArrayList<T>(document.size());

    int index = 0;
    for (Modification<T> modification : modifications) {
      modified.addAll(document.subList(index, modification.getSourceStartIndex()));
      modified.addAll(modification.getTargetLines());
      index = modification.getSourceStartIndex() + modification.getSourceLength();
    }
    modified.addAll(document.subList(index, document.size()));

    return modified;
  }

  public String toString() {
    return (type == TYPE_ADD ? "ADD " : (type == TYPE_REMOVE ? "REMOVE " : "CHANGED ")) +
            sourceStartIndex + ":" + getSourceLength() + " " + targetStartIndex + ":" + getTargetLength();
  }
}
