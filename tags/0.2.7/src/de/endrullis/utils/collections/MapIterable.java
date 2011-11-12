package de.endrullis.utils.collections;

import util.Function1;

/**
 * Iterable that iterators over a another iterator and applies a function to all its elements.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class MapIterable<T1, T2> extends ExtIterable<T2> {
	private ExtIterable<T1> prevIterable;
	private Function1<T1, T2> mapFunc;

	public MapIterable(ExtIterable<T1> prevIterable, Function1<T1, T2> mapFunc) {
		this.prevIterable = prevIterable;
		this.mapFunc = mapFunc;
	}

	@Override
	public ExtIterator<T2> iterator() {
		return prevIterable.iterator().map(mapFunc);
	}
}
