package de.endrullis.utils;

/**
 * Some class like in Scala.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class Some<T> implements Option<T> {
	private T object;

	public Some(T object) {
		this.object = object;
	}

	@Override
	public T get() {
		return object;
	}

	@Override
	public T getOrElse(T alternative) {
		return object;
	}

	@Override
	public boolean isNone() {
		return false;
	}

	@Override
	public boolean isDefined() {
		return true;
	}
}
