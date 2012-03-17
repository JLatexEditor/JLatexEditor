package util;

import de.endrullis.utils.collections.CollectionUtils;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for SimpleTrie.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class SimpleTrieTest extends TestCase {
  public void testTrie() {
    SimpleTrie<Object> root = new SimpleTrie<Object>();
    assertEquals(1, root.add("asdf"));
    assertEquals("asdf", root.getMaxCommonPrefix(""));
    assertEquals("asdf", root.getMaxCommonPrefix("a"));
    assertEquals("asdf", root.getMaxCommonPrefix("asd"));
    assertEquals("asdf", root.getMaxCommonPrefix("asdf"));
    assertEquals("asdf", root.getMaxCommonPrefix("asdff"));
    assertEquals(Arrays.asList("asdf"), root.getStrings("", 1));
    assertEquals(Arrays.asList("asdf"), root.getStrings("", 100));
    assertEquals(Arrays.asList("asdf"), root.getStrings("asd", 100));
    assertEquals(Arrays.asList("asdf"), root.getStrings("asdf", 100));
    assertEquals(Arrays.asList(), root.getStrings("asdff", 100));
    assertEquals("", root.getMaxCommonPrefix("bla"));
    assertEquals("as", root.getMaxCommonPrefix("asbla"));
    assertEquals(true, root.remove("asdf"));
	  assertEquals(1, root.add("asdf", 123));
	  assertEquals(Arrays.asList(123), root.getObjects("", 100));
	  assertEquals(Arrays.asList(123), root.getObjects("asd", 100));
	  assertEquals(Arrays.asList(123), root.getObjects("asdf", 100));
	  assertEquals(Arrays.asList(), root.getObjects("asdff", 100));
    assertEquals(true, root.remove("asdf"));
    assertEquals(1, root.add("asdf"));
    assertEquals(1, root.add("aaa"));
    assertEquals(2, root.add("aaa"));
    assertEquals(1, root.add("blah"));
    assertEquals(false, root.remove("asdf"));
    assertEquals(false, root.remove("aaa"));
    assertEquals(1, root.add("ccc1"));
    assertEquals(1, root.add("ccc5"));
    assertEquals(1, root.add("ccc3"));
    assertEquals(1, root.add("ccc32"));
    assertEquals(1, root.add("ccc31"));
    assertEquals(1, root.add("ccc33"));
    assertEquals(1, root.add("ccc9"));
    assertEquals(1, root.add("d"));
    assertEquals(1, root.add("ccc2"));
    assertEquals(1, root.add("b2"));
    assertEquals("", root.getMaxCommonPrefix(""));
    assertEquals("ccc", root.getMaxCommonPrefix("c"));

    assertEquals(Arrays.asList("aaa", "b2", "blah"), root.getStrings("", 3));


    assertEquals(Arrays.asList("aaa"), root.getStrings("aa", 1));

    assertEquals(Arrays.asList("ccc1", "ccc2", "ccc3"), root.getStrings("c", 3));

    assertEquals(1, root.add("cc"));

    assertEquals(Arrays.asList("cc", "ccc1", "ccc2"), root.getStrings("c", 3));
  }

	public void testObjectIterator() {
		String[] strings = {
			"aba",
			"abadaba",
			"abba",
			"abbadabba",
			"abbanichdoch",
			"abbgemalt",
			"abbgemalter",
			"abbladab",
			"badabu",
			"dabadu",
			"dabaladab",
		};

		SimpleTrie<String> trie = new SimpleTrie<String>();
		for (String string : strings) {
			trie.add(string, string);
		}

		String expected = CollectionUtils.join(Arrays.asList(strings), "\n");
		String actual = CollectionUtils.join(trie.getObjectsIterable(), "\n");
		assertEquals(expected, actual);

		expected = CollectionUtils.join(Arrays.asList(strings).subList(0, 5), "\n");
		actual = CollectionUtils.join(trie.getObjects("", 5), "\n");
		assertEquals(expected, actual);

		expected = CollectionUtils.join(Arrays.asList(strings).subList(0, 0), "\n");
		actual = CollectionUtils.join(trie.getObjects("", 0), "\n");
		assertEquals(expected, actual);

		ArrayList<String> expectedList = new ArrayList<String>();
		for (String string : strings) {
			if (string.startsWith("abb")) expectedList.add(string);
		}
		expected = CollectionUtils.join(expectedList, "\n");
		actual = CollectionUtils.join(trie.getObjectsIterable("abb"), "\n");
		assertEquals(expected, actual);
	}
}
