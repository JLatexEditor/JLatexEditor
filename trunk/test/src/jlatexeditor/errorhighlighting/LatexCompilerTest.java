package jlatexeditor.errorhighlighting;

import jlatexeditor.ErrorView;
import junit.framework.TestCase;

import java.io.*;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class LatexCompilerTest extends TestCase {
	public void testCompiler() throws IOException {
		ErrorView errorView = new ErrorView(null);
		LatexCompiler compiler = LatexCompiler.createInstance(LatexCompiler.Type.pdf, null, errorView);

		compiler.parseLatexOutput(new File("."), new BufferedReader(new InputStreamReader(new FileInputStream("test/resources/latex_output/output1.txt"))));
	}
}
