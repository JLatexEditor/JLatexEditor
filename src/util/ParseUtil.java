package util;

import java.util.ArrayList;

public class ParseUtil {
  public static String parseBalanced(String text, int index, char stop) {
    int start = index;
    int depth = 0;
    while(index < text.length()) {
      char c = text.charAt(index);
      switch (c) {
        case '\\' : index = index+2; continue;
        case '{' : depth++; break;
        case '}' : depth--; if(depth < 0) return text.substring(start, index); else break;
        case '"' : index++;
                   int close = text.indexOf('"', index);
                   if(close == -1) close = text.length();
                   index = close;
                   continue;
      }
      if(c == stop) return text.substring(start, index);
    }
    return text.substring(start, index);
  }

  public static ArrayList<String> splitBySpace(String string) {
    ArrayList<String> parts = new ArrayList<String>();
    int index = 0, lastIndex = 0;
    while((index = string.indexOf(' ', index)) != -1) {
      if(lastIndex < index-1) {
        parts.add(string.substring(lastIndex, index));
      }
      index++;
      lastIndex = index;
    }
    if(lastIndex < string.length()) parts.add(string.substring(lastIndex));
    return parts;
  }
}