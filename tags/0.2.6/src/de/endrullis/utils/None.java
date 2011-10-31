package de.endrullis.utils;

/**
 * None class like in Scala.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class None<T> implements Option<T> {
	@Override
	public T get() {
		throw new RuntimeException("Accessing a non-existing object.");
	}

	@Override
	public T getOrElse(T alternative) {
		return alternative;
	}

	@Override
	public boolean isNone() {
		return true;
	}
}
