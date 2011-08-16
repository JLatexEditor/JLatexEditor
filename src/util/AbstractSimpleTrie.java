package util;

import de.endrullis.utils.collections.ExtIterable;

import java.util.Iterator;

/**
 * Abstract simple trie.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public abstract class AbstractSimpleTrie<T> implements Trie<T>, Iterable<T> {
	public abstract ExtIterable<T> getObjectsIterable(final String prefix);

	@Override
	public Iterator<T> iterator() {
		return getObjectsIterable("").iterator();
	}
}
