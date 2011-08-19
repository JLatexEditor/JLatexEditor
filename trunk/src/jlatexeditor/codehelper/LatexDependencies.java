package jlatexeditor.codehelper;

import jlatexeditor.PackagesExtractor;
import jlatexeditor.SCEManager;
import jlatexeditor.quickhelp.LatexQuickHelp;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

/**
 * Latex dependencies.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class LatexDependencies {

	public static boolean isCurrentDocumentClassProviding(LatexQuickHelp.Element element) {
		BackgroundParser backgroundParser = SCEManager.getBackgroundParser();
		if (backgroundParser.getDocumentClass() != null) {
			String docClass = backgroundParser.getDocumentClass().getName();

			HashSet<? extends PackagesExtractor.ComEnv> comEnvs;
			switch (element.type) {
				case command:
					comEnvs = PackagesExtractor.getDocClassesParser().getCommands().get(element.name);
					break;
				case environment:
					comEnvs = PackagesExtractor.getDocClassesParser().getEnvironments().get(element.name);
					break;
				default:
					throw new NotImplementedException();
			}

			// is document class providing the command / environment
			if (comEnvs != null) {
				for (PackagesExtractor.ComEnv comEnv : comEnvs) {
					if (comEnv.getPack().getName().equals(docClass)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static ArrayList<PackInfo> getPackagesProviding(LatexQuickHelp.Element element) {
		ArrayList<PackInfo> packs = new ArrayList<PackInfo>();

		BackgroundParser backgroundParser = SCEManager.getBackgroundParser();

		HashSet<? extends PackagesExtractor.ComEnv> comEnvs = null;
		switch (element.type) {
			case command:
				comEnvs = PackagesExtractor.getPackageParser().getCommands().get(element.name);
				break;
			case environment:
				comEnvs = PackagesExtractor.getPackageParser().getEnvironments().get(element.name);
				break;
		}

		if (comEnvs != null) {
			// build HashSet with all packages directly and indirectly imported in this document
			HashSet<PackagesExtractor.Package> indirectlyImportedPackagesHash = backgroundParser.getIndirectlyImportedPackages();

			// build HashSet with all packages directly or indirectly providing the given command / environment
			HashSet<PackagesExtractor.Package> dependentPackagesHash = new HashSet<PackagesExtractor.Package>();
			for (PackagesExtractor.ComEnv comEnv : comEnvs) {
				comEnv.getPack().addDependantPackagesRecursively(dependentPackagesHash);
			}

			// build up lists of imported and importable packages providing the command
			ArrayList<PackagesExtractor.Package> importablePackages = new ArrayList<PackagesExtractor.Package>();
			ArrayList<PackagesExtractor.Package> importedPackages = new ArrayList<PackagesExtractor.Package>();
			for (PackagesExtractor.Package pack : dependentPackagesHash) {
				if (indirectlyImportedPackagesHash.contains(pack)) {
					importedPackages.add(pack);
				} else {
					importablePackages.add(pack);
				}
			}
			/*
			for (PackagesExtractor.Command command : commands) {
				PackagesExtractor.Package pack = command.getPack();
				for (PackagesExtractor.Package depPack : pack.getDependantPackagesRecursively()) {
					if (!importedPackages.contains(depPack) && !importablePackages.contains(depPack)) {
						if (backgroundParser.getPackages().contains(pack.getName())) {
							importedPackages.add(pack);
						} else {
							importablePackages.add(pack);
						}
					}
				}
			}
			*/
			Comparator<PackagesExtractor.Package> comparator = new Comparator<PackagesExtractor.Package>() {
				@Override
				public int compare(PackagesExtractor.Package o1, PackagesExtractor.Package o2) {
					if (o1.getUsageCount() > o2.getUsageCount()) return -1;
					if (o1.getUsageCount() == o2.getUsageCount()) return o1.getName().compareToIgnoreCase(o2.getName());
					return 1;
				}
			};
			Collections.sort(importedPackages, comparator);
			Collections.sort(importablePackages, comparator);

			for (PackagesExtractor.Package pack : importedPackages) {
				String descString = pack.getDescription() != null ? pack.getDescription() : "";
				packs.add(new PackInfo(PackInfo.Type.pack, pack, PackInfo.State.imported));
			}
			for (PackagesExtractor.Package pack : importablePackages) {
				packs.add(new PackInfo(PackInfo.Type.pack, pack, PackInfo.State.importable));
			}
		}

		return packs;
	}

	public static class PackInfo {
		enum Type { pack, docclass }
		enum State { imported, importable}

		// yes I'm using public fields because getters and setters in Java are too much Visual overhead -> we really need Scala
		// by the way, this whole class could be defined in Scala by using a single line
		public Type type;
		public PackagesExtractor.Package pack;
		public State state;

		public PackInfo(Type type, PackagesExtractor.Package pack, State state) {
			this.type = type;
			this.pack = pack;
			this.state = state;
		}
	}
}
