package jlatexeditor.errorhighlighting;

import jlatexeditor.ErrorView;
import junit.framework.TestCase;
import util.StreamUtils;
import util.SystemUtils;

import java.io.*;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class LatexCompilerTest extends TestCase {
	public void testCompilerOutput() throws Exception {
		File dir = new File("test/resources/latex_output");
		File[] latexOuts = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".latex-out");
			}
		});

		for (File latexOut : latexOuts) {
			File parseOut = new File(latexOut.getPath().substring(0, latexOut.getPath().length() - 10) + ".parse-out");

			compareParseOutput(latexOut, parseOut);
		}
	}

	private void compareParseOutput(File latexOut, File parseOut) throws Exception {
		ErrorView errorView = new ErrorView(null);
		LatexCompiler compiler = LatexCompiler.createInstance(LatexCompiler.Type.pdf, null, errorView);

		try {
			compiler.parseLatexOutput(new File("."), new BufferedReader(new InputStreamReader(new FileInputStream(latexOut))));
		} catch (Exception e) {
			System.out.println(errorView.getText());
			throw e;
		}

		StringBuilder sb = new StringBuilder();
		for (LatexCompileError latexCompileError : errorView.getErrors()) {
			sb.append(latexCompileError.getTypeString()).append(":\n");
			sb.append(latexCompileError.toString()).append("\n");
		}

		String parseOutContent = StreamUtils.readFile(parseOut.getPath());

		assertEquals(parseOutContent, sb.toString());
	}
}
