package util.diff;

import java.util.List;

/**
 * Diff test.
 */
public class DiffTest {
  public static void main(String args[]) {
    System.out.println("Test 1: ");
    /*
    1123345678910
    2222345678910
    3333345678910
    444443456789
    555554455678
    666665556567
    777776656656
    CHANGED 0 0 1
    REMOVE 1 1 1
    CHANGED 3 2 1
    REMOVE 5 4 2
    REMOVE 10 7 1
    */
    Token[] tokens1 = new TokenList("LAHYQQKPGKA", false).getTokens();
    Token[] tokens2 = new TokenList("YHCQPGK", false).getTokens();

    List<Modification> modifications = new Diff().diff(tokens1, tokens2);
    for(Modification modification : modifications) {
      System.out.println(modification);
    }
    
  }
}
