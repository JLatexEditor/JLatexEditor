package util;

import java.util.List;

/**
 * Abstract Trie implementation.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public interface AbstractTrie<T> {
	public int add(String s);
	public int add(String s, T object);

	public String getMaxCommonPrefix(String prefix);
	public List<String> getStrings(String prefix, int count);
	public List<T> getObjects(String prefix, int count);
}
