package util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Helper functions for working with arrays.
 */
public class ArrayUtil {
  /**
   * Convert a collection into an array.
   */
  public static <T> T[] toArray(Collection<T> collection) {
    T array[] = (T[]) new Object[collection.size()];
    collection.toArray(array);
    return array;
  }

  public static String[] toStringArray(Collection<String> collection) {
    String array[] = new String[collection.size()];
    collection.toArray(array);
    return array;
  }

  /**
   * Convert an array into an ArrayList.
   */
  public static <T> ArrayList<T> toArrayList(T[] array) {
    ArrayList<T> arrayList = new ArrayList<T>();
    for(T element : array) arrayList.add(element);
    return arrayList;
  }

  /**
   * Prepend an element.
   */
  public static <T> T[] add(T element, T[] array) {
    T[] result = (T[]) new Object[array.length + 1];
    System.arraycopy(array, 0, result, 1, array.length);
    result[0] = element;
    return result;
  }

  /**
   * Concat the arrays.
   */
  public static <T> T[] concatArray(T[]... arrays) {
    int length = 0;
    for(T[] array : arrays) length += array.length;

    T[] result = (T[]) new Object[length];
    int index = 0;
    for(T[] array : arrays) {
      System.arraycopy(array, 0, result, index, array.length);
      index += array.length;
    }
    return result;
  }

  public static char[] concatCharArray(char[]... arrays) {
    int length = 0;
    for(char[] array : arrays) length += array.length;

    char[] result = new char[length];
    int index = 0;
    for(char[] array : arrays) {
      System.arraycopy(array, 0, result, index, array.length);
      index += array.length;
    }
    return result;
  }

  /**
   * Set union of all elements using equals.
   */
  public static <T> T[] unionArray(T[]... arrays) {
    ArrayList<T> union = new ArrayList<T>();
    for(T[] array : arrays) for(T element : array) {
      if(!union.contains(element)) union.add(element);
    }

    T[] result = (T[]) new Object[union.size()];
    union.toArray(result);
    return result;
  }

  /**
   * Convert the char array to a string array.
   */
  public static String[] charArrayToStringArray(char a[]) {
    String b[] = new String[a.length];
    for(int i = 0; i < a.length; i++) b[i] = new String(new char[] {a[i]});
    return b;
  }

  /**
   * Drop the first n elements from the list.
   */
  public static <T> T[] drop(T[] array, int n) {
    T[] result = (T[]) new Object[array.length - n];
    System.arraycopy(array, n, result, 0, result.length);
    return result;
  }
}