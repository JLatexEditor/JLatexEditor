package util;

import java.util.Comparator;
import java.util.Iterator;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class SortedMergeIterable<T> implements Iterable<T> {
	private Comparator<T> comparator;
	private Iterable<T>[] iterables;

	public SortedMergeIterable(Comparator<T> comparator, Iterable<T>... iterables) {
		this.comparator = comparator;
		this.iterables = iterables;
	}

	@Override
	public Iterator<T> iterator() {
		Iterator<T>[] iterators = new Iterator[iterables.length];
		for (int i = 0; i < iterators.length; i++) {
			iterators[i] = iterables[i].iterator();
		}
		return new SortedMergeIterator<T>(comparator, iterators);
	}
}
