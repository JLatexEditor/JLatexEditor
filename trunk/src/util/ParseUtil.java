package util;

import static de.endrullis.utils.Tuple.*;

import java.util.ArrayList;

public class ParseUtil {
	/**
	 * Reads a single Latex command, an balanced argument between two braces or a single character,
	 *
	 * @param text input text
	 * @param index start index
	 * @return (character / command / argument, nextIndex)
	 */
	public static Tuple2<String,Integer> parseItem(String text, int index) {
		if (text.charAt(index) == '{') {
			String argument = parseBalanced(text, index + 1, '}');
			return _(argument, index + argument.length() + 2);
		} else {
			while (text.charAt(index) == ' ') index++;
			if (text.charAt(index) == '\\') {
				int startIndex = index;
				index++;
				if (Character.isLetter(text.charAt(index))) {
					while (Character.isLetter(text.charAt(index))) index++;
				} else {
					index++;
				}
				return _(text.substring(startIndex, index), index);
			} else {
				return _("" + text.charAt(index), index + 1);
			}
		}
	}
	
  /**
   * Parses a balanced string excluding the closing symbol. 
   */
  public static String parseBalanced(String text, int index, char stop) {
    int start = index;
    int depth = 0;
    while (index < text.length()) {
      char c = text.charAt(index);
      switch (c) {
        case '\\':
          index = index + 2;
          continue;
        case '{':
          depth++;
          index++;
          continue;
        case '}':
          depth--;
          if (depth < 0) {
            return text.substring(start, index);
          } else {
            index++;
            continue;
          }
        case '"':
          index++;
          int close = text.indexOf('"', index);
          if (close == -1) close = text.length() - 1;
          index = close + 1;
          continue;
      }
      if (depth == 0 && c == stop) return text.substring(start, index);
      index++;
    }
    return text.substring(start, index);
  }

  public static ArrayList<String> splitBySpace(String string) {
    ArrayList<String> parts = new ArrayList<String>();
    int index = 0, lastIndex = 0;
    while ((index = string.indexOf(' ', index)) != -1) {
      if (lastIndex < index - 1) {
        parts.add(string.substring(lastIndex, index).toLowerCase());
      }
      index++;
      lastIndex = index;
    }
    if (lastIndex < string.length()) parts.add(string.substring(lastIndex).toLowerCase());
    return parts;
  }
}
