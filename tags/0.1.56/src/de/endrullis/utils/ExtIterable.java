package de.endrullis.utils;

import util.Function1;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Extended iterable.
 * Poor Java that we have to write those classes ourselves.
 * We should really use Scala in future.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public abstract class ExtIterable<T> implements Iterable<T> {
	public ArrayList<T> toList() {
		Iterator<T> iter = iterator();
		ArrayList<T> list = new ArrayList<T>();
		while (iter.hasNext()) {
			list.add(iter.next());
		}
		return list;
	}

	public ArrayList<T> toList(int maxElements) {
		Iterator<T> iter = iterator();
		ArrayList<T> list = new ArrayList<T>();
		for (int i=0; i<maxElements && iter.hasNext(); i++) {
			list.add(iter.next());
		}
		return list;
	}

	public <T2> ExtIterable<T2> map(Function1<T, T2> mapFunc) {
		return new MapIterable<T, T2>(this, mapFunc);
	}

	@Override
	public abstract ExtIterator<T> iterator();
}
