package util;

import de.endrullis.utils.collections.Equals;
import de.endrullis.utils.collections.ExtIterable;
import de.endrullis.utils.collections.MergeSortIterable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Comparator;
import java.util.List;

/**
 * Priority-based merged Trie consisting of multiple Tries.
 * Sorry for incompleteness of this implementation, but we need a mix-in composition (e.g. traits like in Scala) here
 * in order to design a reasonable object-oriented model of Trie implementations.
 * Welcome at the end of Java.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class SimpleMergedTrie<T> extends AbstractSimpleTrie<T> {
	private Comparator<T> comparator;
	private Equals<T> equals;
	private AbstractSimpleTrie<T>[] tries;

	public SimpleMergedTrie(final Comparator<T> comparator, AbstractSimpleTrie<T>... tries) {
		this.comparator = comparator;
		this.equals = new Equals<T>() {
			public boolean equals(T a1, T a2) {
				return comparator.compare(a1, a2) == 0;
			}
		};
		this.tries = tries;
	}

	@Override
	public ExtIterable<T> getObjectsIterable(String prefix) {
		Iterable<T>[] iters = new Iterable[tries.length];
		for (int i = 0; i < iters.length; i++) {
			iters[i] = tries[i].getObjectsIterable(prefix);
		}
		return new MergeSortIterable<T>(comparator, iters).distinct(equals);
	}

	@Override
	public T get(String key) {
		for (AbstractSimpleTrie<T> trie : tries) {
			T object = trie.get(key);
			if (object != null) return object;
		}
		return null;
	}

	@Override
	public int add(String s) {
		throw new NotImplementedException();
	}

	@Override
	public int add(String s, T object) {
		throw new NotImplementedException();
	}

	@Override
	public String getMaxCommonPrefix(String prefix) {
		String maxPrefix = null;
		for (AbstractSimpleTrie<T> trie : tries) {
			String maxCommonPrefix = trie.getMaxCommonPrefix(prefix);
			if (maxCommonPrefix.length() >= prefix.length()) {
				if (maxPrefix == null) {
					maxPrefix = maxCommonPrefix;
				} else {
					maxPrefix = maxCommonPrefix(maxPrefix, maxCommonPrefix);
				}
			}
		}
		return maxPrefix;
	}

	@Override
	public List<String> getStrings(String prefix, int count) {
		throw new NotImplementedException();
	}

	@Override
	public boolean contains(String key) {
		for (AbstractSimpleTrie<T> trie : tries) {
			if (trie.contains(key)) return true;
		}
		return false;
	}

	public static String maxCommonPrefix(String prefix1, String prefix2) {
		char[] chars1 = prefix1.toCharArray();
		char[] chars2 = prefix2.toCharArray();

		int i;
		for (i = 0; i < chars1.length && i < chars2.length; i++) {
			if (chars1[i] != chars2[i]) {
				return prefix1.substring(0, i);
			}
		}

		return prefix1.substring(0, i);
	}
}
