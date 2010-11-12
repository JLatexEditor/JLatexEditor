package jlatexeditor;

import util.ParamsExt2;

/**
 * JLatexEditor params.
 *
 * @author Stefan Endrullis
 */
public class JLatexEditorParams extends ParamsExt2 {
	public JLatexEditorParams() {
		syntax("jlatexeditor [" + underline("Options") + "] " +
			"[" + underline("File1") + "[" + bold(":") + underline("LineNumber") + "] " +
			"[" + underline("File2") + "[" + bold(":") + underline("LineNumber") + "]] ...]");
	}

	public Option color   = option("color",    'c', "sets color mode for output (" + bold("on") + ", " + bold("off") + ", or " + bold("auto") + ")", 1);
	public Option help    = option("help",     'h', "prints help dialog");
	public Option version = option("version",  'v', "prints version of " + bold("JLatexEditor"));
}
