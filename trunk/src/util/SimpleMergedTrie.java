package util;

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
	private AbstractSimpleTrie<T>[] tries;

	public SimpleMergedTrie(Comparator<T> comparator, AbstractSimpleTrie<T>... tries) {
		this.comparator = comparator;
		this.tries = tries;
	}

	@Override
	public ExtIterable<T> getObjectsIterable(String prefix) {
		Iterable<T>[] iters = new Iterable[tries.length];
		for (int i = 0; i < iters.length; i++) {
			iters[i] = tries[i].getObjectsIterable(prefix);
		}
		return new MergeSortIterable<T>(comparator, iters);
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
