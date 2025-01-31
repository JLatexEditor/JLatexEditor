package util;

import de.endrullis.utils.collections.ExtIterable;
import de.endrullis.utils.collections.ExtIterator;

import java.util.*;

/**
 * Simple Trie implementation that maps keys to objects.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class SimpleTrie<T> extends AbstractSimpleTrie<T> {
  private int count = 0;
  private TreeMap<Character, SimpleTrie<T>> map = new TreeMap<Character, SimpleTrie<T>>();

  private T object;

  public SimpleTrie() {
    this(null);
  }

  public SimpleTrie(T object) {
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

    SimpleTrie<T> next = map.get(chars[i]);
    if (next == null) {
      next = new SimpleTrie<T>();
      map.put(chars[i], next);
    }
    return next.add(chars, i + 1, object);
  }

  public boolean remove(String s) {
    return remove(truncate(s).toCharArray(), 0, false);
  }

  public boolean removeAll(String s) {
    return remove(truncate(s).toCharArray(), 0, true);
  }

  private boolean remove(char[] chars, boolean all) {
    return remove(chars, 0, all);
  }

  private boolean remove(char[] chars, int i, boolean all) {
    if (i == chars.length) {
	    if (all) {
		    count = 0;
		    return true;
	    } else {
		    return --count == 0;
	    }
    }

    SimpleTrie next = map.get(chars[i]);
    if (next != null && next.remove(chars, i + 1, all)) {
      map.remove(chars[i]);
    }
    return map.isEmpty();
  }

  public T get(String s) {
    return get(truncate(s).toCharArray());
  }

  private T get(char[] chars) {
	  SimpleTrie<T> node = getNode(chars, 0);
	  return node != null ? node.object : null;
  }

  private SimpleTrie<T> getNode(String prefix) {
	  return getNode(prefix.toCharArray(), 0);
  }

  private SimpleTrie<T> getNode(char[] chars, int i) {
    if (i == chars.length) return this;

    SimpleTrie<T> next = map.get(chars[i]);
    return next != null ? next.getNode(chars, i + 1) : null;
  }

  public String getMaxCommonPrefix(String prefix) {
    return getMaxCommonPrefix(truncate(prefix).toCharArray(), 0);
  }

  private String getMaxCommonPrefix(char[] chars, int i) {
    if (i == chars.length) {
      return getMaxCommonPrefix();
    }

    SimpleTrie next = map.get(chars[i]);
    if (next == null) {
      return "";
    }

    return chars[i] + next.getMaxCommonPrefix(chars, i + 1);
  }

  private String getMaxCommonPrefix() {
    if (count == 0 && map.size() == 1) {
      Map.Entry<Character, SimpleTrie<T>> entity = map.entrySet().iterator().next();
      return entity.getKey() + entity.getValue().getMaxCommonPrefix();
    }
    return "";
  }

  public List<String> getStrings(String prefix, int count) {
    // navigate to the trie node that represents the end of the prefix
    char[] chars = truncate(prefix).toCharArray();
    SimpleTrie<T> t = this;
    for (char aChar : chars) {
      t = t.map.get(aChar);
      if (t == null) {
        return new ArrayList<String>();
      }
    }

    ArrayList<String> list = new ArrayList<String>();
    t.addStrings(list, prefix, count);
    return list;
  }

	public ExtIterable<T> getObjectsIterable(final String prefix) {
		return new ExtIterable<T>() {
			@Override
			public ExtIterator<T> iterator() {
				return new TrieIterator<T>(getNode(prefix)).map(new Function1<SimpleTrie<T>, T>() {
					@Override
					public T apply(SimpleTrie<T> a1) {
						return a1.object;
					}
				});
			}
		};
	}

	public class TrieIterator<T> extends ExtIterator<SimpleTrie<T>> {
		private LinkedList<Iterator<SimpleTrie<T>>> nodeStack = new LinkedList<Iterator<SimpleTrie<T>>>();
		private SimpleTrie<T> trie;

		public TrieIterator(SimpleTrie<T> initialNode) {
			trie = initialNode;
			if (trie != null && !trie.map.isEmpty()) {
				nodeStack.push(trie.map.values().iterator());
			}
		}

		@Override
		public boolean hasNext() {
			if (trie != null && trie.hasObject()) return true;

			while (!nodeStack.isEmpty()) {
				Iterator<SimpleTrie<T>> peek = nodeStack.peek();
				if (peek.hasNext()) {
					trie = peek.next();
					if (!trie.map.isEmpty()) {
						nodeStack.push(trie.map.values().iterator());
					}
					if (trie.hasObject()) return true;
				} else {
					nodeStack.pop();
				}
			}

			trie = null;
			return false;
		}

		@Override
		public SimpleTrie<T> next() {
			if (hasNext()) {
				SimpleTrie<T> ret = trie;
				trie = null;
				return ret;
			}
			return null;
		}
	}

	private boolean hasObject() {
		return count > 0;
	}

	private int addStrings(List<String> list, String prefix, int remaining) {
    if (count > 0) {
      list.add(prefix);
      remaining--;
    }
    for (Map.Entry<Character, SimpleTrie<T>> entry : map.entrySet()) {
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
    for (Map.Entry<Character, SimpleTrie<T>> entry : map.entrySet()) {
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

	public int count(String key) {
		SimpleTrie<T> node = getNode(key);
		return node == null ? 0 : node.count;
	}
}
