
package editor.errorhighlighting;




/**
 * @author Jörg Endrullis
 */

public interface LatexCompileListener{
  public void compileStarted();

  public void compileEnd();

  public void latexError(LatexCompileError error);
}
