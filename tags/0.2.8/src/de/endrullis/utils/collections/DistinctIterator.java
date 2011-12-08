package de.endrullis.utils.collections;

import de.endrullis.utils.collections.ExtIterator;

import java.util.Comparator;
import java.util.Iterator;

/**
 * Iterator for getting distinct values.
 * Note that the values in the inner iterator have to be sorted.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class DistinctIterator<T> extends ExtIterator<T> {
	private Iterator<T> iterator;
	private Equals<T> equals;
	private T lastValue, next;

	public DistinctIterator(Iterator<T> iterator, Equals<T> equals) {
		this.iterator = iterator;
		this.equals = equals;
	}

	public boolean hasNext() {
		while (next == null) {
			if (!iterator.hasNext()) return false;
			next = iterator.next();
			if (lastValue != null && equals.equals(lastValue, next)) {
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
