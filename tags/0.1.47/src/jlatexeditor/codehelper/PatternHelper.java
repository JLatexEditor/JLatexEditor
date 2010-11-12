package jlatexeditor.codehelper;

import sce.codehelper.CodeHelper;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;

import java.util.List;

/**
 * Pattern based code helper.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public abstract class PatternHelper extends CodeHelper {
  protected PatternPair pattern;
  protected List<WordWithPos> params;

  public boolean matches() {
    params = pattern.find(pane);
    return params != null;
  }
}
