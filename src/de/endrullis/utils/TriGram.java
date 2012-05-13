package de.endrullis.utils;

import java.util.HashSet;

/**
 * TriGram string similarity.
 */
public class TriGram {
  private HashSet<Long> gramsX = new HashSet<Long>();
  private HashSet<Long> gramsY = new HashSet<Long>();

  public HashSet<Long> trigrams(String s) {
    HashSet<Long> grams = new HashSet<Long>();
    trigrams(s, grams);
    return grams;
  }

  public void trigrams(String s, HashSet<Long> grams) {
    grams.clear();
    long value = 0;
    for(int index = 0; index < s.length(); index++) {
      value = ((value & 0xFFFFFFFFl) << 16) | s.charAt(index);
      grams.add(value);
    }
  }

  public double compare(String x, String y) {
    trigrams(x, gramsX);
    return compare(gramsX, y);
  }

  public double compare(HashSet<Long> gramsX, String y) {
    trigrams(y, gramsY);
    int sizeY = gramsY.size();
    gramsY.retainAll(gramsX);
    return (2d * gramsY.size()) / (gramsX.size() + sizeY);
  }
}
