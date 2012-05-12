package jlatexeditor.errorhighlighting;

/**
 * Empty implementation of LatexCompilerListener.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class LatexCompilerAdapter implements LatexCompileListener {
	@Override
	public void compileStarted() {
	}

	@Override
	public void compileEnd() {
	}

	@Override
	public void latexError(LatexCompileError error) {
	}
}
