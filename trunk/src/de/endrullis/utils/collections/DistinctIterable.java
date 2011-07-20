package de.endrullis.utils.collections;

/**
 * Iterable for getting distinct values.
 * Note that the values need to be sorted.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class DistinctIterable<T> extends ExtIterable<T> {
	private Iterable<T> iterable;

	public DistinctIterable(Iterable<T> iterable) {
		this.iterable = iterable;
	}

	@Override
	public ExtIterator<T> iterator() {
		return new DistinctIterator<T>(iterable.iterator());
	}
}
