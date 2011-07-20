package de.endrullis.utils.collections;

import de.endrullis.utils.collections.ExtIterator;

import java.util.Iterator;

/**
 * Iterator for getting distinct values.
 * Note that the values in the inner iterator have to be sorted.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class DistinctIterator<T> extends ExtIterator<T> {
	private Iterator<T> iterator;
	private T lastValue, next;

	public DistinctIterator(Iterator<T> iterator) {
		this.iterator = iterator;
	}

	public boolean hasNext() {
		while (next == null) {
			if (!iterator.hasNext()) return false;
			next = iterator.next();
			if (lastValue != null && lastValue.equals(next)) {
				next = null;
			}
		}
		return true;
	}

	public T next() {
		if (next == null) hasNext();
		lastValue = next;
		next = null;
		return lastValue;
	}
}
