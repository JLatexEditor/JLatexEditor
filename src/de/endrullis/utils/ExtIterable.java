package de.endrullis.utils;

import com.sun.org.apache.xpath.internal.functions.Function;
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
		return CollectionUtils.take(this, maxElements);
	}

	public <T2> ExtIterable<T2> map(Function1<T, T2> mapFunc) {
		return new MapIterable<T, T2>(this, mapFunc);
	}

	public ExtIterable<T> filter(Function1<T, Boolean> filterFunc) {
		return new FilterIterable<T>(this, filterFunc);
	}

	@Override
	public abstract ExtIterator<T> iterator();
}
