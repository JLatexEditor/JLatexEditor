package util.diff;

import util.StreamUtils;
import util.diff.levenstein.LevenStein;
import util.diff.levenstein.Modification;
import util.diff.levenstein.Token;
import util.diff.levenstein.TokenList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Diff test.
 */
public class DiffTest {
  public static void main(String args[]) throws IOException {
    {
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

      List<Modification<Token>> modifications = new LevenStein().diff(tokens1, tokens2);
      for (Modification modification : modifications) {
        System.out.println(modification);
      }
    }

    {
      System.out.println("Test 2: ");
      TokenList[] file1 = readFile("test-src/util/diff/diff1.tex");
      TokenList[] file2 = readFile("test-src/util/diff/diff2.tex");

      long startTime = System.nanoTime();
      List<Modification<TokenList>> modifications = new LevenStein().diff(file1, file2);
      long endTime = System.nanoTime();
      for (Modification modification : modifications) {
        System.out.println(modification);
      }
      System.out.println("time: " + (((endTime - startTime) / 10000000) / 100.));
    }
  }

  /*
  private static Token[] readFile(String fileName) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(StreamUtils.getInputStream(fileName)));

    ArrayList<Token> lines = new ArrayList<Token>();
    String line;
    while((line = reader.readLine()) != null) {
      lines.add(new Token(line));
    }

    Token[] linesList = new Token[lines.size()];
    lines.toArray(linesList);
    return linesList;
  }
  */

  private static TokenList[] readFile(String fileName) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(StreamUtils.getInputStream(fileName)));

    ArrayList<TokenList> lines = new ArrayList<TokenList>();
    String line;
    while ((line = reader.readLine()) != null) {
      lines.add(new TokenList(line, true));
    }

    TokenList[] linesList = new TokenList[lines.size()];
    lines.toArray(linesList);
    return linesList;
  }
}
