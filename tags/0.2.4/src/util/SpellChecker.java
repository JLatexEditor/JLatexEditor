package util;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

/**
 * Spell checker interface.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public interface SpellChecker {
	public Result check(String word) throws IOException;
	public HashSet<String> getPersonalWords() throws IOException;
	public void addToPersonalDict(String word) throws IOException;
	public void removeFromPersonalDict(String word) throws IOException;
	public void shutdown();

	/**
	 * Spell check result.
	 */
	class Result {
	  private boolean correct;
	  private List<String> suggestions;

	  /**
	   * Creates a correct result.
	   */
	  public Result() {
	    correct = true;
	  }

	  /**
	   * Creates a result with suggestions.
	   *
	   * @param suggestions list of suggestions
	   */
	  public Result(List<String> suggestions) {
	    this.suggestions = suggestions;
	    correct = false;
	  }

	  public boolean isCorrect() {
	    return correct;
	  }

	  public List<String> getSuggestions() {
	    return suggestions;
	  }

	  @Override
	  public String toString() {
	    return correct ?
	            "correct" :
	            "misspelled; suggestions: " + suggestions;
			}
		}
}
