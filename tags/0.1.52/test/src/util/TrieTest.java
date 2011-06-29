package util;

import junit.framework.TestCase;
import util.Trie;

import java.util.List;

/**
 * Tests for Trie.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class TrieTest extends TestCase {
  public void testTrie() {
    Trie<Object> root = new Trie<Object>();
    assertEquals(1, root.add("asdf"));
    assertEquals("asdf", root.getMaxCommonPrefix(""));
    assertEquals("asdf", root.getMaxCommonPrefix("a"));
    assertEquals("asdf", root.getMaxCommonPrefix("asd"));
    assertEquals("asdf", root.getMaxCommonPrefix("asdf"));
    assertEquals("", root.getMaxCommonPrefix("bla"));
    assertEquals("as", root.getMaxCommonPrefix("asbla"));
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

    List<String> list = root.getStrings("", 3);
    String s = "";
    for (String s1 : list) {
      s += s1 + "\n";
    }
    assertEquals(
            "aaa\n" +
						"b2\n" +
						"blah\n", s);

    list = root.getStrings("aa", 1);
    s = "";
    for (String s1 : list) {
      s += s1 + "\n";
    }
    assertEquals("aaa\n", s);

    list = root.getStrings("c", 3);
    s = "";
    for (String s1 : list) {
      s += s1 + "\n";
    }
    assertEquals(
            "ccc1\n" +
						"ccc2\n" +
						"ccc3\n", s);

    assertEquals(1, root.add("cc"));

    list = root.getStrings("c", 3);
    s = "";
    for (String s1 : list) {
      s += s1 + "\n";
    }
    assertEquals(
            "cc\n" +
						"ccc1\n" +
						"ccc2\n", s);
  }
}