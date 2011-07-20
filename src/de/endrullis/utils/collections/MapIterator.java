package de.endrullis.utils.collections;

import util.Function1;

import java.util.Iterator;

/**
 * Iterator that iterators over a another iterator and applies a function to all its elements.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class MapIterator<T1, T2> extends ExtIterator<T2> {
	private Iterator<T1> prevIterator;
	private Function1<T1, T2> mapFunc;

	public MapIterator(Iterator<T1> prevIterator, Function1<T1, T2> mapFunc) {
		this.prevIterator = prevIterator;
		this.mapFunc = mapFunc;
	}

	@Override
	public boolean hasNext() {
		return prevIterator.hasNext();
	}

	@Override
	public T2 next() {
		T1 next = prevIterator.next();
		if (next == null) return null;
		return mapFunc.apply(next);
	}

	@Override
	public void remove() {
		prevIterator.remove();
	}
}
