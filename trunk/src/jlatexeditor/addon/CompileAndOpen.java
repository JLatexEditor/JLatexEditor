package jlatexeditor.addon;

import jlatexeditor.JLatexEditorJFrame;
import jlatexeditor.errorhighlighting.LatexCompileError;
import jlatexeditor.errorhighlighting.LatexCompileListener;
import jlatexeditor.errorhighlighting.LatexCompiler;
import jlatexeditor.errorhighlighting.LatexCompilerAdapter;
import sce.codehelper.WordWithPos;
import sce.component.SCEPane;

import java.util.Iterator;

/**
 * Close current environment.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CompileAndOpen extends AddOn {
	protected CompileAndOpen() {
		super("build pdf and open", "Build pdf and Open Document", "alt SPACE");
	}

	@Override
	public void run(final JLatexEditorJFrame jle) {
		jle.saveAll();
		LatexCompiler compiler = jle.compile(LatexCompiler.Type.pdf);

		if (compiler == null) return;

		compiler.addLatexCompileListener(new LatexCompilerAdapter() {
			@Override
			public void compileEnd() {
				jle.performForwardSearch();
			}
		});
	}
}
