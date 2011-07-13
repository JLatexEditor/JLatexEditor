package jlatexeditor.codehelper;

import de.endrullis.utils.CollectionUtils;
import jlatexeditor.PackagesExtractor;
import jlatexeditor.gproperties.GPropertiesCodeHelper;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import util.Function1;

import java.util.ArrayList;
import java.util.List;

/**
 * CodeHelper for \\begin{...} and \\end{...}.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class EnvironmentCodeHelper extends PatternHelper {
  protected WordWithPos word;

  public EnvironmentCodeHelper() {
	  pattern = new PatternPair("\\\\(?:begin|end)\\s*\\{([^{}]*)");
  }

	@Override
	public boolean matches() {
	  if (super.matches()) {
	    word = params.get(0);
	    return true;
	  }
	  return false;
	}

	@Override
	public WordWithPos getWordToReplace() {
	  return word;
	}

	@Override
	public Iterable<? extends CHCommand> getCompletions() {
	  return getCompletions(word.word);
	}

	@Override
	public String getMaxCommonPrefix() {
	  return getMaxCommonPrefix(word.word);
	}

	public Iterable<CHCommand> getCompletions(String search) {
		// TODO merge envs of docclasses and envs of packages
		List<String> envNames = PackagesExtractor.getPackageParser().getEnvironments().getStrings(search, 20);
		if (envNames == null) envNames = new ArrayList<String>();

		return CollectionUtils.map(envNames, new Function1<String, CHCommand>() {
			public CHCommand apply(String packageName) {
				return new GPropertiesCodeHelper.ValueCompletion(packageName);
			}
		});
	}

	public String getMaxCommonPrefix(String search) {
		// TODO
	  return search;
	}
}
