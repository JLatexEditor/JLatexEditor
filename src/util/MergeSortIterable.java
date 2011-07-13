package util;

import de.endrullis.utils.ExtIterable;
import de.endrullis.utils.ExtIterator;

import java.util.Comparator;
import java.util.Iterator;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class MergeSortIterable<T> extends ExtIterable<T> {
	private Comparator<T> comparator;
	private Iterable<T>[] iterables;

	public MergeSortIterable(Comparator<T> comparator, Iterable<T>... iterables) {
		this.comparator = comparator;
		this.iterables = iterables;
	}

	@Override
	public ExtIterator<T> iterator() {
		Iterator<T>[] iterators = new Iterator[iterables.length];
		for (int i = 0; i < iterators.length; i++) {
			iterators[i] = iterables[i].iterator();
		}
		return new SortedMergeIterator<T>(comparator, iterators);
	}
}
