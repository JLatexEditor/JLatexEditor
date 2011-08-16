package util;

import de.endrullis.utils.collections.Equals;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Tests for SimpleMergedTrie.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class SimpleMergedTrieTest extends TestCase {
	public void test() {
		SimpleTrie<Item> trie1 = getTrie(1,
			"a",
			"c",
			"d",
			"e"
		);
		SimpleTrie<Item> trie2 = getTrie(2,
			"b",
			"c",
			"e"
		);

		SimpleMergedTrie<Item> mergedTrie = new SimpleMergedTrie<Item>(new Comparator<Item>() {
			@Override
			public int compare(Item o1, Item o2) {
				return o1.name.compareTo(o2.name);
			}
		}, trie1, trie2);

		assertEquals(Arrays.asList(
			new Item("a", 1),
			new Item("b", 2),
			new Item("c", 1),
			new Item("d", 1),
			new Item("e", 1)
		), mergedTrie.getObjectsIterable().distinct(new Equals<Item>() {
			public boolean equals(Item a1, Item a2) {
				return a1.name.equals(a2.name);
			}
		}).toList());
	}

	private SimpleTrie<Item> getTrie(int owner, String... names) {
		SimpleTrie<Item> trie = new SimpleTrie<Item>();

		for (String name : names) {
			addItem(trie, name, owner);
		}

		return trie;
	}

	private void addItem(SimpleTrie<Item> trie, String name, int owner) {
		trie.add(name, new Item(name, owner));
	}

	class Item {
		String name;
		int owner;

		Item(String name, int owner) {
			this.name = name;
			this.owner = owner;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Item) {
				Item that = (Item) obj;
				return this.name.equals(that.name) && this.owner == that.owner;
			}
			return false;
		}

		@Override
		public String toString() {
			return "Item(" + name + ", " + owner + ")";
		}
	}
}
