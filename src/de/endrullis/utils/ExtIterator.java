package de.endrullis.utils;

import util.Function1;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Extended iterator.
 * Poor Java that we have to write those classes ourselves.
 * We should really use Scala in future.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public abstract class ExtIterator<T> implements Iterator<T> {
	public ArrayList<T> toList() {
		ArrayList<T> list = new ArrayList<T>();
		while (hasNext()) {
			list.add(next());
		}
		return list;
	}

	public ArrayList<T> toList(int maxElements) {
		ArrayList<T> list = new ArrayList<T>();
		for (int i=0; i<maxElements && hasNext(); i++) {
			list.add(next());
		}
		return list;
	}

	public <T2> ExtIterator<T2> map(Function1<T, T2> mapFunc) {
		return new MapIterator<T, T2>(this, mapFunc);
	}
}
