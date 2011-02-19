package de.endrullis.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Configuration properties.  This class is the Java version of my ScalaProperties.
 * The Java version is unfortunately 4 times longer then the Scala one.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class ConfigProperties extends BetterProperties2 {
	public void comment (String text) {
		addEntry(new Comment(text));
	}

	public abstract class Property<T> {
		protected String name;
		protected T value;
		protected T default_;
		protected String altDefault;
		protected List<PropertyListener<T>> listeners = new ArrayList<PropertyListener<T>>();

		protected Property(String name, T default_, String altDefault) {
			this.name = name;
			this.value = default_;
			this.default_ = default_;
			this.altDefault = altDefault;
		}

		protected abstract T read();
		protected abstract void write(T value);

		public T get() {
			return value;
		}
		public void set(T value) {
			this.value = value;
			write(value);
		}
		public void update() {
			T newValue = read();
			boolean update = (value == null && newValue != null) ||
			                 (value != null && newValue == null) ||
			                 (value != null && !value.equals(newValue));
			if (update) {
				value = newValue;
				informListeners();
			}
		}
	  protected void informListeners() {
		  for (PropertyListener<T> listener : listeners) {
			  listener.onValueChanged(value);
		  }
	  }
	}

	public static interface PropertyListener<T> {
		public void onValueChanged(T value);
	}

	public class StringProperty extends Property<String> {
		public StringProperty(String name, String default_) {
			this(name, default_, null);
		}

		public StringProperty(String name, String default_, String altDefault) {
			super(name, default_, altDefault);
			addEntry(new Def(name, new PString(), default_, altDefault));
		}

		protected void write(String value) {
			setString(name, value);
		}
		public String read() {
			return getString(value);
		}
	}

	public class IntProperty extends Property<Integer> {
		private int min;
		private int max;

		public IntProperty(String name, Integer default_) {
			this(name, default_, null);
		}

		public IntProperty(String name, Integer default_, String altDefault) {
			this(name, default_, altDefault, Integer.MIN_VALUE, Integer.MAX_VALUE);
		}

		public IntProperty(String name, Integer default_, String altDefault, int min, int max) {
			super(name, default_, altDefault);
			this.min = min;
			this.max = max;

			addEntry(new Def(name, new PInt(min, max), "" + default_, altDefault));
		}

		public void write(Integer value) {
			setInt(name, value);
		}
		public Integer read() {
			return getInt(name);
		}
	}

	public class DoubleProperty extends Property<Double> {
		private double min;
		private double max;

		public DoubleProperty(String name, Double default_) {
			this(name, default_, null);
		}

		public DoubleProperty(String name, Double default_, String altDefault) {
			this(name, default_, altDefault, Double.MIN_VALUE, Double.MAX_VALUE);
		}

		public DoubleProperty(String name, Double default_, String altDefault, double min, double max) {
			super(name, default_, altDefault);
			this.min = min;
			this.max = max;

			addEntry(new Def(name, new PDouble(min, max), "" + default_, altDefault));
		}

		protected void write(Double value) {
			setDouble(name, value);
		}
		public Double read() {
			return getDouble(name);
		}
	}

	public class BooleanProperty extends Property<Boolean> {
		public BooleanProperty(String name, Boolean default_) {
			this(name, default_, null);
		}

		public BooleanProperty(String name, Boolean default_, String altDefault) {
			super(name, default_, altDefault);
			addEntry(new Def(name, new PBoolean(), "" + default_, altDefault));
		}

		protected void write(Boolean value) {
			setBoolean(name, value);
		}
		protected Boolean read() {
			return getBoolean(name);
		}
	}

	public class LogLevelProperty extends Property<Level> {
		public LogLevelProperty(String name, Level default_) {
			this(name, default_, null);
		}

		public LogLevelProperty(String name, Level default_, String altDefault) {
			super(name, default_, altDefault);
			addEntry(new Def(name, new PBoolean(), "" + default_, altDefault));
		}

		protected void write(Level value) {
			setString(name, value.getName());
		}
		protected Level read() {
			String string = getString(name);
			return string == null ? null : Level.parse(string);
		}
	}
}
