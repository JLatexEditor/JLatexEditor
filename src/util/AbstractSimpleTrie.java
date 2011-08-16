package util;

import de.endrullis.utils.collections.ExtIterable;

import java.util.Iterator;
import java.util.List;

/**
 * Abstract simple trie.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public abstract class AbstractSimpleTrie<T> implements Trie<T>, Iterable<T> {
	public abstract ExtIterable<T> getObjectsIterable(final String prefix);

	public ExtIterable<T> getObjectsIterable() {
		return getObjectsIterable("");
	}

	@Override
	public List<T> getObjects(String prefix, int count) {
		return getObjectsIterable(prefix).toList(count);
	}

	@Override
	public Iterator<T> iterator() {
		return getObjectsIterable("").iterator();
	}
}
