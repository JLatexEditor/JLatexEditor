package de.endrullis.utils;

import java.util.ArrayList;

/**
 * Utilities for Strings.
 *
 * @author Jörg Endrullis und Stefan Endrullis
 * @version 1.2
 */
public class StringUtils{
  public static String stringBefore(String inStr, String delimiterStr){
    int pos = inStr.indexOf(delimiterStr);
    if(pos == -1)
      return null;
    else
      return inStr.substring(0, pos);
  }

  /**
   * Returns the string before a delimiter string.
   *
   * @param inStr input string
   * @param delimiterStr delimiter
   * @param nr 'f' for first, 'l' for last
   * @return string before a delimiter string
   */
  public static String stringBefore(String inStr, String delimiterStr, char nr){
    if(nr == 'f') {
      return stringBefore(inStr, delimiterStr);
    }
    else {
      int pos = inStr.lastIndexOf(delimiterStr);
      if(pos == -1)
        return null;
      else
        return inStr.substring(0, pos);
    }
  }

  public static String stringAfter(String inStr, String delimiterStr){
    int pos = inStr.indexOf(delimiterStr);
    if(pos == -1)
      return null;
    else
      return inStr.substring(pos + delimiterStr.length(), inStr.length());
  }

  /**
   * Returns the string after a delimiter string.
   *
   * @param inStr input string
   * @param delimiterStr delimiter
   * @param nr 'f' for first, 'l' for last
   * @return string after a delimiter string
   */
  public static String stringAfter(String inStr, String delimiterStr, char nr){
    if(nr == 'f') {
      return stringAfter(inStr, delimiterStr);
    }
    else {
      int pos = inStr.lastIndexOf(delimiterStr);
      if(pos == -1)
        return null;
      else
        return inStr.substring(pos + delimiterStr.length(), inStr.length());
    }
  }

  public static String[] stringSplitter(String inStr, String delimiterStr){
    ArrayList<String> stringVector = new ArrayList<String>();
    String beforeStr;
    while((beforeStr = stringBefore(inStr, delimiterStr)) != null){
      stringVector.add(beforeStr);
      inStr = stringAfter(inStr, delimiterStr);
    }
    stringVector.add(inStr);

    return stringVector.toArray(new String[]{});
  }

	/**
	 * Truncate the strings to a maximal length of 30.
	 *
	 * @param s string
	 * @return truncated string
	 */
	public static String truncate(String s) {
		if (s.length() > 30) {
			return s.substring(0, 30);
		} else {
			return s;
		}
	}

	/**
	 * Tokenizes a text.
	 *
	 * @param text text
	 * @return list of tokens
	 */
	public static ArrayList<String> tokenize(String text) {
		ArrayList<String> tokens = new ArrayList<String>();
		char[] chars = text.toCharArray();

		boolean quote = false;
		StringBuffer token = new StringBuffer();

		for (int i=0; i<text.length(); i++) {
			switch (chars[i]) {
				case ' ':
					if (quote) {
						token.append(chars[i]);
					} else {
						if (token.length() > 0) {
							tokens.add(token.toString());
							token = new StringBuffer();
						}
					}
					break;

				case '\\':
					i++;
					token.append(chars[i]);
					break;

				case '"':
					quote = !quote;
					break;

				default:
					token.append(chars[i]);
			}
		}

		if (token.length() > 0) {
			tokens.add(token.toString());
		}

		return tokens;
	}
}
