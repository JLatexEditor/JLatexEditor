package de.endrullis.utils;

/**
 * Some Scala tuples for Java.  For comfortable use import all members via static imports:
 * <pre>
 *   import static de.endrullis.utils.Tuple.*;
 * </pre>
 *
 * Than you can instantiate new tuple via
 * <pre>
 *   t(firstValue, secondValue, ...)
 * </pre>
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class Tuple {

	public static class Tuple2<T1,T2> {
		public T1 _1;
		public T2 _2;

		public Tuple2(T1 _1, T2 _2) {
			this._1 = _1;
			this._2 = _2;
		}
	}

	public static class Tuple3<T1,T2,T3> {
		public T1 _1;
		public T2 _2;
		public T3 _3;

		public Tuple3(T1 _1, T2 _2, T3 _3) {
			this._1 = _1;
			this._2 = _2;
			this._3 = _3;
		}
	}

	public static class Tuple4<T1,T2,T3,T4> {
		public T1 _1;
		public T2 _2;
		public T3 _3;
		public T4 _4;

		public Tuple4(T1 _1, T2 _2, T3 _3, T4 _4) {
			this._1 = _1;
			this._2 = _2;
			this._3 = _3;
			this._4 = _4;
		}
	}

	public static class Tuple5<T1,T2,T3,T4,T5> {
		public T1 _1;
		public T2 _2;
		public T3 _3;
		public T4 _4;
		public T5 _5;

		public Tuple5(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5) {
			this._1 = _1;
			this._2 = _2;
			this._3 = _3;
			this._4 = _4;
			this._5 = _5;
		}
	}

	public static class Tuple6<T1,T2,T3,T4,T5,T6> {
		public T1 _1;
		public T2 _2;
		public T3 _3;
		public T4 _4;
		public T5 _5;
		public T6 _6;

		public Tuple6(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6) {
			this._1 = _1;
			this._2 = _2;
			this._3 = _3;
			this._4 = _4;
			this._5 = _5;
			this._6 = _6;
		}
	}

	public static class Tuple7<T1,T2,T3,T4,T5,T6,T7> {
		public T1 _1;
		public T2 _2;
		public T3 _3;
		public T4 _4;
		public T5 _5;
		public T6 _6;
		public T7 _7;

		public Tuple7(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7) {
			this._1 = _1;
			this._2 = _2;
			this._3 = _3;
			this._4 = _4;
			this._5 = _5;
			this._6 = _6;
			this._7 = _7;
		}
	}

	public static class Tuple8<T1,T2,T3,T4,T5,T6,T7,T8> {
		public T1 _1;
		public T2 _2;
		public T3 _3;
		public T4 _4;
		public T5 _5;
		public T6 _6;
		public T7 _7;
		public T8 _8;

		public Tuple8(T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8) {
			this._1 = _1;
			this._2 = _2;
			this._3 = _3;
			this._4 = _4;
			this._5 = _5;
			this._6 = _6;
			this._7 = _7;
			this._8 = _8;
		}
	}

	public static <T1,T2> Tuple2<T1,T2> t(T1 t1, T2 t2) {
		return new Tuple2<T1,T2>(t1, t2);
	}

	public static <T1,T2,T3> Tuple3<T1,T2,T3> t(T1 t1, T2 t2, T3 t3) {
		return new Tuple3<T1,T2,T3>(t1, t2, t3);
	}

	public static <T1,T2,T3,T4> Tuple4<T1,T2,T3,T4> t(T1 t1, T2 t2, T3 t3, T4 t4) {
		return new Tuple4<T1,T2,T3,T4>(t1, t2, t3, t4);
	}

	public static <T1,T2,T3,T4,T5> Tuple5<T1,T2,T3,T4,T5> t(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
		return new Tuple5<T1,T2,T3,T4,T5>(t1, t2, t3, t4, t5);
	}

	public static <T1,T2,T3,T4,T5,T6> Tuple6<T1,T2,T3,T4,T5,T6> t(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
		return new Tuple6<T1,T2,T3,T4,T5,T6>(t1, t2, t3, t4, t5, t6);
	}

	public static <T1,T2,T3,T4,T5,T6,T7> Tuple7<T1,T2,T3,T4,T5,T6,T7> t(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
		return new Tuple7<T1,T2,T3,T4,T5,T6,T7>(t1, t2, t3, t4, t5, t6, t7);
	}

	public static <T1,T2,T3,T4,T5,T6,T7,T8> Tuple8<T1,T2,T3,T4,T5,T6,T7,T8> t(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) {
		return new Tuple8<T1,T2,T3,T4,T5,T6,T7,T8>(t1, t2, t3, t4, t5, t6, t7, t8);
	}

}
