package jlatexeditor.codehelper;

import de.endrullis.utils.collections.CollectionUtils;
import de.endrullis.utils.collections.ExtIterable;
import jlatexeditor.PackagesExtractor;
import sce.codehelper.CHCommand;
import sce.codehelper.PatternPair;
import sce.codehelper.WordWithPos;
import util.Function1;
import util.Trie;
import util.TrieSet;

import java.util.ArrayList;
import java.util.List;

/**
 * CodeHelper for \\usepackage{...}.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class UsePackageCodeHelper extends ExtPatternHelper<PackagesExtractor.Package> {
	private static final Function1<PackagesExtractor.Package,CHCommand> PACKAGE_2_CHCOMMAND = new Function1<PackagesExtractor.Package, CHCommand>() {
		@Override
		public CHCommand apply(PackagesExtractor.Package a1) {
			return new ValueCompletion(a1.getName());
		}
	};

	public UsePackageCodeHelper() {
		super("packages");
		pattern = new PatternPair("\\\\usepackage(?:\\[[^\\]]*\\])?\\{([^{},]+,)*([^{},]*)");
	}

	@Override
	public boolean matches() {
	  if (super.matches()) {
	    word = params.get(1);
	    return true;
	  }
	  return false;
	}

	public Iterable<CHCommand> getCompletions(String search, Function1<PackagesExtractor.Package, Boolean> filterFunc) {
		ExtIterable<PackagesExtractor.Package> packIter = PackagesExtractor.getPackageParser().getPackages().getObjectsIterable(search);
		if (packIter == null) return new ArrayList<CHCommand>();

		return packIter.filter(filterFunc).map(PACKAGE_2_CHCOMMAND).toList(20);
	}

	protected Function1<PackagesExtractor.Package, Boolean> minUsage(final int minUsageCount) {
		return new Function1<PackagesExtractor.Package, Boolean>() {
			@Override
			public Boolean apply(PackagesExtractor.Package pack) {
				return pack.getUsageCount() >= minUsageCount;
			}
		};
	}

	public String getMaxCommonPrefix(String search) {
		String maxCommonPrefix = PackagesExtractor.getPackageParser().getPackages().getMaxCommonPrefix(search);
		if (maxCommonPrefix.length() > search.length()) {
			return maxCommonPrefix;
		}
	  return search;
	}
}
