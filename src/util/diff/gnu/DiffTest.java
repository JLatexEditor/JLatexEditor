/*
 * $Log: DiffTest.java,v $
 * Revision 1.4  2007/12/20 05:04:21  stuart
 * Can no longer import from default package.
 *
 * Revision 1.3  2006/09/28 17:10:41  stuart
 * New Diff test case.  DateDelta.
 *
 * Revision 1.2  2004/01/29 02:35:35  stuart
 * Test for out of bounds exception in UnifiedPrint.print_hunk.
 * Add setOutput() to DiffPrint.Base.
 *
 * Revision 1.1  2003/05/29 20:22:40  stuart
 * Test EditMask.getFix()
 *
 */
package util.diff.gnu;

import junit.framework.TestCase;

import java.io.StringWriter;

/**
 * Test Diff behavior.
 *
 * @author Stuart D. Gathman
 *         Copyright (C) 2002 Business Management Systems, Inc.
 */
public class DiffTest extends TestCase {

  private static String[] f1 = {"hello"};
  private static String[] f2 = {"hello", "bye"};


  public DiffTest(String name) {
    super(name);
  }

  public void testReverse() {
    Diff diff = new Diff(f1, f2);
    Diff.change script = diff.diff_2(true);
    assertTrue(script != null);
    assertTrue(script.link == null);
  }

  /**
   * For Java versions without auto-boxing.
   */
  private Integer[] loadArray(int[] a) {
    Integer[] b = new Integer[a.length];
    for (int i = 0; i < a.length; ++i)
      b[i] = new Integer(a[i]);
    return b;
  }

  /**
   * This was causing an array out of bounds exception.
   * Submitted by Markus Oellinger.
   */
  public void testSwap() {
    final Integer[] l1 = loadArray(new int[]{1, 2, 4, 7, 9, 35, 56, 58, 76});
    final Integer[] l2 = loadArray(new int[]{1, 2, 4, 76, 9, 35, 56, 58, 7});
    Diff diff = new Diff(l1, l2);
    Diff.change script = diff.diff_2(false);
    // script should have two changes
    assertTrue(script != null);
    assertTrue(script.link != null);
    assertTrue(script.link.link == null);
    assertEquals(1, script.inserted);
    assertEquals(1, script.deleted);
    assertEquals(3, script.line0);
    assertEquals(3, script.line1);
    assertEquals(1, script.link.inserted);
    assertEquals(1, script.link.deleted);
    assertEquals(8, script.link.line0);
    assertEquals(8, script.link.line1);
    //DiffPrint.Base p = new DiffPrint.UnifiedPrint(l1,l2);
    //p.print_script(script);
  }

  private static String[] test1 = {
          "aaa", "bbb", "ccc", "ddd", "eee", "fff", "ggg", "hhh", "iii"
  };
  private static String[] test2 = {
          "aaa", "jjj", "kkk", "lll", "bbb", "ccc", "hhh", "iii", "mmm", "nnn", "ppp"
  };

  /**
   * Test context based output.  Changes past the end of old file
   * were causing an array out of bounds exception.
   * Submitted by Cristian-Augustin Saita and Adam Rabung.
   */
  public void testContext() {
    Diff diff = new Diff(test1, test2);
    Diff.change script = diff.diff_2(false);
    DiffPrint.Base p = new DiffPrint.UnifiedPrint(test1, test2);
    StringWriter wtr = new StringWriter();
    p.setOutput(wtr);
    //p.print_header("test1","test2");
    p.print_script(script);
    /* FIXME: when DiffPrint is updated to diff-2.7, testfor expected
       output in wtr.toString(). diff-1.15 does not combine adjacent
       changes when they are close together. */
  }

}
