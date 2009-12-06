package util.diff;

import java.util.ArrayList;

/**
 * Tokenizer.
 */
public class Token implements Metric<Token> {
  private String string;

  public Token(String string) {
    this.string = string;
  }

  public int getDistance(Token a, int max) {
    return a.string.equals(string) ? 0 : max;
  }
}

