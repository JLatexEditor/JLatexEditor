package jlatexeditor.gproperties;

import de.endrullis.utils.BetterProperties2.Range;
import de.endrullis.utils.BetterProperties2.PSet;
import jlatexeditor.GProperties;
import jlatexeditor.codehelper.PatternHelper;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class GPropertiesCodeHelper extends PatternHelper {
	protected String key;
	protected WordWithPos value;
	protected LinkedHashSet<String> range;

  public GPropertiesCodeHelper() {
	  pattern = new PatternPair("^([^#=]+)=([^#]*)");
  }

	@Override
	public boolean matches() {
		if (super.matches()) {
			key = params.get(0).word;
			value = params.get(1);

			Range range = GProperties.getRange(key);
			if (range instanceof PSet) {
				PSet set = (PSet) range;
				this.range = set.content;
				return true;
			}
		}
		return false;
	}

	@Override
	public WordWithPos getWordToReplace() {
		return value;
	}

	@Override
	public Iterable<? extends CHCommand> getCompletions() {
		return getCompletions(key, value.word);
	}

	@Override
	public String getMaxCommonPrefix() {
		return getMaxCommonPrefix(key, value.word);
	}

	public Iterable<ValueCompletion> getCompletions(String key, final String prefix) {
		ArrayList<ValueCompletion> list = new ArrayList<ValueCompletion>();

		GProperties.getRange(key);

		for (String value : range) {
			if (value.startsWith(prefix)) {
				list.add(new ValueCompletion(value));
			}
		}

    return list;
  }

	/**
	 * Searches for the best completion of the prefix.
	 *
	 * @param key property name
	 * @param fileName filename
	 * @return the completion suggestion (without the prefix)
	 */
	public String getMaxCommonPrefix(String key, String fileName) {
	  int prefixLength = fileName.length();
	  String completion = null;

		for (CHCommand command : getCompletions(key, fileName)) {
			String commandName = command.getName();
			if (commandName.startsWith(fileName)) {
				if (completion == null) {
					completion = commandName;
				} else {
					// find the common characters
					int commonIndex = prefixLength;
					int commonLength = Math.min(completion.length(), commandName.length());
					while (commonIndex < commonLength) {
						if (completion.charAt(commonIndex) != commandName.charAt(commonIndex)) break;
						commonIndex++;
					}
					completion = completion.substring(0, commonIndex);
				}
			}
		}

	  return completion;
	}

	public static class ValueCompletion extends CHCommand {
		/**
		 * Creates a command with the given name.
		 *
		 * @param name the name
		 */
		public ValueCompletion(String name) {
			super(name);
		}
	}
}