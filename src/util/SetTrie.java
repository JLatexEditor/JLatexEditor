package util;

import de.endrullis.utils.collections.ExtIterable;
import de.endrullis.utils.collections.ExtIterator;

import java.util.*;

/**
 * Trie implementation that maps keys to sets of objects.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class SetTrie<T> implements Trie<T> {
	private TreeMap<Character, SetTrie<T>> map = new TreeMap<Character, SetTrie<T>>();

	private HashSet<T> objects = new HashSet<T>();

	public SetTrie() {
	}

	public SetTrie(T object) {
	  objects.add(object);
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
	  if(i == chars.length) {
		  objects.add(object);
		  return objects.size();
	  }

	  SetTrie<T> next = map.get(chars[i]);
	  if (next == null) {
	    next = new SetTrie<T>();
	    map.put(chars[i], next);
	  }
	  return next.add(chars, i + 1, object);
	}

	public boolean remove(String s, T object) {
	  return remove(truncate(s).toCharArray(), object, 0);
	}

	private boolean remove(char[] chars, T object) {
	  return remove(chars, object, 0);
	}

	private boolean remove(char[] chars, T object, int i) {
	  if (i == chars.length) {
		  objects.remove(object);
	    return objects.size() == 0;
	  }

	  SetTrie<T> next = map.get(chars[i]);
	  if (next != null && next.remove(chars, object, i + 1)) {
	    map.remove(chars[i]);
	  }
	  return map.isEmpty();
	}

	public HashSet<T> get(String s) {
	  return get(truncate(s).toCharArray(), 0);
	}

	private HashSet<T> get(char[] chars) {
	  return get(chars, 0);
	}

	public HashSet<T> getOrEmpty(String cmd) {
		HashSet<T> ts = get(cmd);
		if (ts == null) {
			return new HashSet<T>();
		}
		return ts;
	}

	private SetTrie<T> getNode(String prefix) {
		return getNode(prefix.toCharArray(), 0);
	}

	private SetTrie<T> getNode(char[] chars, int i) {
	  if (i == chars.length) return this;

	  SetTrie<T> next = map.get(chars[i]);
	  return next != null ? next.getNode(chars, i + 1) : null;
	}

	private HashSet<T> get(char[] chars, int i) {
	  if (i == chars.length) return objects;

	  SetTrie<T> next = map.get(chars[i]);
	  return next != null ? next.get(chars, i + 1) : null;
	}

	public String getMaxCommonPrefix(String prefix) {
	  return getMaxCommonPrefix(truncate(prefix).toCharArray(), 0);
	}

	private String getMaxCommonPrefix(char[] chars, int i) {
	  if (i == chars.length) {
	    return getMaxCommonPrefix();
	  }

	  SetTrie<T> next = map.get(chars[i]);
	  if (next == null) {
	    return "";
	  }

	  return chars[i] + next.getMaxCommonPrefix(chars, i + 1);
	}

	private String getMaxCommonPrefix() {
	  if (objects.size() == 0 && map.size() == 1) {
	    Map.Entry<Character, SetTrie<T>> entity = map.entrySet().iterator().next();
	    return entity.getKey() + entity.getValue().getMaxCommonPrefix();
	  }
	  return "";
	}

	public List<String> getStrings(String prefix, int count) {
	  // navigate to the trie node that represents the end of the prefix
	  char[] chars = truncate(prefix).toCharArray();
	  SetTrie<T> t = this;
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

	public Iterator<SetTrie<T>> getTrieSetIterator() {
		return getTrieSetIterator("").iterator();
	}

	public ExtIterable<SetTrie<T>> getTrieSetIterator(final String prefix) {
		return new ExtIterable<SetTrie<T>>() {
			@Override
			public ExtIterator<SetTrie<T>> iterator() {
				return new TrieIterator<T>(getNode(prefix));
			}
		};
	}


	public class TrieIterator<T> extends ExtIterator<SetTrie<T>> {
		private LinkedList<Iterator<SetTrie<T>>> nodeStack = new LinkedList<Iterator<SetTrie<T>>>();
		private SetTrie<T> trie;

		public TrieIterator(SetTrie<T> initialNode) {
			trie = initialNode;
			if (trie != null && !trie.map.isEmpty()) {
				nodeStack.push(trie.map.values().iterator());
			}
		}

		@Override
		public boolean hasNext() {
			if (trie != null && trie.hasObjects()) return true;

			while (!nodeStack.isEmpty()) {
				Iterator<SetTrie<T>> peek = nodeStack.peek();
				if (peek.hasNext()) {
					trie = peek.next();
					if (!trie.map.isEmpty()) {
						nodeStack.push(trie.map.values().iterator());
					}
					if (trie.hasObjects()) return true;
				} else {
					nodeStack.pop();
				}
			}

			trie = null;
			return false;
		}

		@Override
		public SetTrie<T> next() {
			if (hasNext()) {
				SetTrie<T> ret = trie;
				trie = null;
				return ret;
			}
			return null;
		}
	}

	private boolean hasObjects() {
		return objects.size() > 0;
	}

	public List<T> getObjects(String prefix, int count) {
	  // navigate to the trie node that represents the end of the prefix
	  char[] chars = truncate(prefix).toCharArray();
	  SetTrie<T> t = this;
	  for (char aChar : chars) {
	    t = t.map.get(aChar);
	    if (t == null) {
	      return new ArrayList<T>();
	    }
	  }

		LinkedHashSet<T> set = new LinkedHashSet<T>();
	  t.addObjects(set, prefix, count);

		ArrayList<T> list = new ArrayList<T>();
		for (T object : set) {
			list.add(object);
		}
	  return list;
	}

	public HashSet<T> getObjects() {
		return objects;
	}

	private int addStrings(List<String> list, String prefix, int remaining) {
	  if (objects.size() > 0) {
	    list.add(prefix);
	    remaining--;
	  }
	  for (Map.Entry<Character, SetTrie<T>> entry : map.entrySet()) {
	    if (remaining == 0) break;
	    remaining = entry.getValue().addStrings(list, prefix + entry.getKey(), remaining);
	  }
	  return remaining;
	}

	private int addObjects(LinkedHashSet<T> list, String prefix, int remaining) {
	  if (objects.size() > 0) {
		  for (T object : objects) {
			  list.add(object);
			  remaining--;
			  if (remaining == 0) break;
		  }
	  }
	  for (Map.Entry<Character, SetTrie<T>> entry : map.entrySet()) {
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
