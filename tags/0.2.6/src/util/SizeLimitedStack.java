package util;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Handles the closed documents.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class SizeLimitedStack<T> implements Iterable<T> {
	private int maxSize;
	private LinkedList<T> queue = new LinkedList<T>();

	public SizeLimitedStack(int maxSize) {
		this.maxSize = maxSize;
	}

	public void push(T element) {
		queue.remove(element);
		queue.push(element);
		if (queue.size() > maxSize) {
			queue.removeLast();
		}
	}

	public boolean isEmpty() {
		return queue.isEmpty();
	}

	public T pop() {
		return queue.pop();
	}

	public void remove(T element) {
		queue.remove(element);
	}

	public void clear() {
		queue.clear();
	}

	@Override
	public Iterator<T> iterator() {
		return queue.iterator();
	}
}
