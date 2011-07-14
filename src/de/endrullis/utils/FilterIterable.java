package de.endrullis.utils;

import util.Function1;

/**
 * Filtered iterable.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class FilterIterable<T> extends ExtIterable<T> {
	private ExtIterable<T> iterable;
	private Function1<T, Boolean> filterFunc;

	public FilterIterable(ExtIterable<T> iterable, Function1<T, Boolean> filterFunc) {
		this.iterable = iterable;
		this.filterFunc = filterFunc;
	}

	@Override
	public ExtIterator<T> iterator() {
		return new FilterIterator<T>(iterable.iterator(), filterFunc);
	}
}
