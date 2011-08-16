package de.endrullis.utils.collections;

import java.util.Comparator;

/**
 * Iterable for getting distinct values.
 * Note that the values need to be sorted.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class DistinctIterable<T> extends ExtIterable<T> {
	private Iterable<T> iterable;
	private Equals<T> equals;

	public DistinctIterable(Iterable<T> iterable) {
		this.iterable = iterable;
		this.equals = Equals.NATURAL_EQUALITY; // we would not have this type generic warning in Scala -> damn Java
	}

	public DistinctIterable(Iterable<T> iterable, Equals<T> equals) {
		this.iterable = iterable;
		this.equals = equals;
	}

	@Override
	public ExtIterator<T> iterator() {
		return new DistinctIterator<T>(iterable.iterator(), equals);
	}
}
