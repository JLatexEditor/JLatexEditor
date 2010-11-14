package util;

import java.util.*;

/**
 * A simple Trie implementation.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class Trie<T> {
  private int count = 0;
  private TreeMap<Character, Trie<T>> map = new TreeMap<Character, Trie<T>>();

  private T object;

  public Trie() {
    this(null);
  }

  public Trie(T object) {
    this.object = object;
  }

  public int add(String s) {
    return add(truncate(s).toCharArray(), 0);
  }

  public int add(String s, T object) {
    return add(truncate(s).toCharArray(), 0, object);
  }

  private int add(char[] chars) {
    return add(chars, null);
  }

  private int add(char[] chars, T object) {
    return add(chars, 0, object);
  }

  private int add(char[] chars, int i) {
    return add(chars, i, null);
  }

  private int add(char[] chars, int i, T object) {
    if(i == chars.length) { this.object = object; return ++count; }

    Trie<T> next = map.get(chars[i]);
    if (next == null) {
      next = new Trie<T>();
      map.put(chars[i], next);
    }
    return next.add(chars, i + 1, object);
  }

  public boolean remove(String s) {
    return remove(truncate(s).toCharArray(), 0);
  }

  private boolean remove(char[] chars) {
    return remove(chars, 0);
  }

  private boolean remove(char[] chars, int i) {
    if (i == chars.length) {
      return --count == 0;
    }

    Trie next = map.get(chars[i]);
    if (next != null && next.remove(chars, i + 1)) {
      map.remove(chars[i]);
    }
    return map.isEmpty();
  }

  public T get(String s) {
    return get(truncate(s).toCharArray(), 0);
  }

  private T get(char[] chars) {
    return get(chars, 0);
  }

  private T get(char[] chars, int i) {
    if (i == chars.length) return object;

    Trie<T> next = map.get(chars[i]);
    return next != null ? next.get(chars, i + 1) : null;
  }

  public String getMaxCommonPrefix(String prefix) {
    return getMaxCommonPrefix(truncate(prefix).toCharArray(), 0);
  }

  private String getMaxCommonPrefix(char[] chars, int i) {
    if (i == chars.length) {
      return getMaxCommonPrefix();
    }

    Trie next = map.get(chars[i]);
    if (next == null) {
      return "";
    }

    return chars[i] + next.getMaxCommonPrefix(chars, i + 1);
  }

  private String getMaxCommonPrefix() {
    if (count == 0 && map.size() == 1) {
      Map.Entry<Character, Trie<T>> entity = map.entrySet().iterator().next();
      return entity.getKey() + entity.getValue().getMaxCommonPrefix();
    }
    return "";
  }

  public List<String> getStrings(String prefix, int count) {
    // navigate to the trie node that represents the end of the prefix
    char[] chars = truncate(prefix).toCharArray();
    Trie<T> t = this;
    for (char aChar : chars) {
      t = t.map.get(aChar);
      if (t == null) {
        return null;
      }
    }

    ArrayList<String> list = new ArrayList<String>();
    t.addStrings(list, prefix, count);
    return list;
  }

  public List<T> getObjects(String prefix, int count) {
    // navigate to the trie node that represents the end of the prefix
    char[] chars = truncate(prefix).toCharArray();
    Trie<T> t = this;
    for (char aChar : chars) {
      t = t.map.get(aChar);
      if (t == null) {
        return null;
      }
    }

    ArrayList<T> list = new ArrayList<T>();
    t.addObjects(list, prefix, count);
    return list;
  }

  private int addStrings(List<String> list, String prefix, int remaining) {
    if (count > 0) {
      list.add(prefix);
      remaining--;
    }
    for (Map.Entry<Character, Trie<T>> entry : map.entrySet()) {
      if (remaining == 0) break;
      remaining = entry.getValue().addStrings(list, prefix + entry.getKey(), remaining);
    }
    return remaining;
  }

  private int addObjects(List<T> list, String prefix, int remaining) {
    if (count > 0) {
      list.add(object);
      remaining--;
    }
    for (Map.Entry<Character, Trie<T>> entry : map.entrySet()) {
      if (remaining == 0) break;
      remaining = entry.getValue().addObjects(list, prefix + entry.getKey(), remaining);
    }
    return remaining;
  }

	/**
	 * Truncate the strings to a maximal length of 30.
	 *
	 * @param s string
	 * @return truncated string
	 */
	private String truncate(String s) {
		if (s.length() > 50) {
			return s.substring(0, 50);
		} else {
			return s;
		}
	}

	public boolean contains(String key) {
		return get(key) != null;
	}
}
