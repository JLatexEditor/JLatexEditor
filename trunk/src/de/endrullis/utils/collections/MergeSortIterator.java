package de.endrullis.utils.collections;

import junit.framework.Test;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Priority-based merge sort iterator.
 * If two elements are equal the merge sort operator prefers the one produced by the iterator with the smaller index in
 * the iterators array.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class MergeSortIterator<T> extends ExtIterator<T> {
	private Comparator<T> comparator;
	private PriorityQueue<IteratorWrapper> queue;

	public MergeSortIterator(final Comparator<T> comparator, Iterator<T>[] iterators) {
		queue = new PriorityQueue<IteratorWrapper>(iterators.length);
		this.comparator = comparator;
		int wrapperNr = 0;
		for (Iterator<T> iterator : iterators) {
			IteratorWrapper wrapper = new IteratorWrapper(iterator, wrapperNr);
			if (wrapper.hasNext()) {
				queue.add(wrapper);
			}
			wrapperNr++;
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
		private int wrapperNr;
		private T next = null;

		private IteratorWrapper(Iterator<T> iterator, int wrapperNr) {
			this.iterator = iterator;
			this.wrapperNr = wrapperNr;
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
			int compare = comparator.compare(this.next, that.next);
			if (compare == 0) {
				return new Integer(this.wrapperNr).compareTo(that.wrapperNr);
			}
			return compare;
		}
	}
}
