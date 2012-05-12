package de.endrullis.utils;

/**
 * Option class like in Scala.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public interface Option<T> {
	/**
	 * Returns the inner object or null if the option is none.
	 *
	 * @return inner object or null if the option is none
	 */
	public T get();

	/**
	 * Returns the inner object or the alternative if the option is none.
	 *
	 * @param alternative alternative returned when the option is none
	 * @return inner object or the alternative if the option is none
	 */
	public T getOrElse(T alternative);

	/**
	 * Returns true if the option is none.
	 *
	 * @return true if the option is none
	 */
	public boolean isNone();

	/**
	 * Returns true if the option has an value (is not none).
	 *
	 * @return true if the option has an value (is not none)
	 */
	public boolean isDefined();
}
