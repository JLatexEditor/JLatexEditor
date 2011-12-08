package de.endrullis.utils;

/**
 * Lazy value.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public abstract class LazyVal<T> {
	private T val = null;
	
	public T getValue() {
		if (val == null) {
			val = calcValue();
		}
		return val;
	}
	
	protected abstract T calcValue();
}
