package util.diff;

import java.util.ArrayList;

/**
 * List of tokens.
 */
public class TokenList implements Metric<TokenList> {
  private Token[] tokenList;

  /**
   * Creates a token list.
   *
   * @param string string to tokenize
   * @param words if true, then split sting in words, otherwise in characters
   */
  public TokenList(String string, boolean words) {
    ArrayList<Token> tokens = new ArrayList<Token>();

    if(words) {
      // split string in words
      for(int charNr = 0; charNr < string.length(); charNr++) {
        char c = string.charAt(charNr);

        // ignore space and tab
        if(c == ' ' || c == '\t') continue;

        // words
        if(('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')) {
          int begin = charNr;
          while(++charNr < string.length()) {
            c = string.charAt(charNr);
            if(!('a' <= c && c <= 'z') && !('A' <= c && c <= 'Z')) break;
          }
          tokens.add(new Token(string.substring(begin, charNr)));
          charNr--;
          continue;
        }

        // numbers
        if('0' <= c && c <= '9') {
          int begin = charNr;
          while(++charNr < string.length()) {
            c = string.charAt(charNr);
            if(!('0' <= c && c <= '9')) break;
          }
          tokens.add(new Token(string.substring(begin, charNr)));
          charNr--;
          continue;
        }

        // everything else
        tokens.add(new Token(string.substring(charNr, charNr+1)));
      }
    } else {
      // split string in characters
      for(int charNr = 0; charNr < string.length(); charNr++) {
        tokens.add(new Token(string.substring(charNr, charNr+1)));
      }
    }

    tokenList = new Token[tokens.size()];
    tokens.toArray(tokenList);
  }

  public Token[] getTokens() {
    return tokenList;
  }

  public int getDistance(TokenList list, int max) {
    int costs = new Diff().costs(this.tokenList, list.tokenList);
    int length = Math.min(Math.max(1, tokenList.length), Math.max(1, list.tokenList.length));
    return Math.min(max, (max * costs) / length);
  }
}
