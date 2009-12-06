package util.diff;

/**
 * Metric between objects.
 */
public interface Metric<T> {
  public int getDistance(T a, int max);
}
