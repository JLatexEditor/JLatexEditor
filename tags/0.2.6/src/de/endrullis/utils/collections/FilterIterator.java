package de.endrullis.utils.collections;

import util.Function1;

/**
 * Filtered iterator.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class FilterIterator<T> extends ExtIterator<T> {
	private ExtIterator<T> iterator;
	private Function1<T, Boolean> filterFunc;
	private T next;

	public FilterIterator(ExtIterator<T> iterator, Function1<T, Boolean> filterFunc) {
		this.iterator = iterator;
		this.filterFunc = filterFunc;
	}

	@Override
	public boolean hasNext() {
		while (next == null && iterator.hasNext()) {
			next = iterator.next();
			if (!filterFunc.apply(next)) {
				next = null;
			}
		}
		return next != null;
	}

	@Override
	public T next() {
		if (hasNext()) {
			T theNext = next;
			next = null;
			return theNext;
		} else {
			return null;
		}
	}
}
