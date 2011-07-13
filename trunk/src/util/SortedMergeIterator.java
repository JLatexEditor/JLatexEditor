package util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class SortedMergeIterator<T> implements Iterator<T> {
	private Comparator<T> comparator;
	private PriorityQueue<IteratorWrapper> queue;

	public SortedMergeIterator(final Comparator<T> comparator, Iterator<T>[] iterators) {
		queue = new PriorityQueue<IteratorWrapper>(iterators.length);
		this.comparator = comparator;
		for (Iterator<T> iterator : iterators) {
			IteratorWrapper wrapper = new IteratorWrapper(iterator);
			if (wrapper.hasNext()) {
				queue.add(wrapper);
			}
		}
	}

	@Override
	public boolean hasNext() {
		return !queue.isEmpty();
	}

	@Override
	public T next() {
		IteratorWrapper top = queue.poll();
		T next = top.next();
		if (top.hasNext()) {
			queue.add(top);
		}
		return next;
	}

	@Override
	public void remove() {
	}

	private class IteratorWrapper implements Comparable<IteratorWrapper> {
		private Iterator<T> iterator;
		private T next = null;

		private IteratorWrapper(Iterator<T> iterator) {
			this.iterator = iterator;
		}

		public boolean hasNext() {
			if (next == null) {
				if (!iterator.hasNext()) return false;
				next = iterator.next();
			}
			return true;
		}

		public T next() {
			if (next == null) hasNext();
			T theNext = next;
			next = null;
			return theNext;
		}

		@Override
		public int compareTo(IteratorWrapper that) {
			return comparator.compare(this.next, that.next);
		}
	}
}
