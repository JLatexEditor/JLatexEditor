package jlatexeditor;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import util.StreamUtils;
import util.Trie;
import util.TrieSet;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class PackagesExtractor {
	private static final String PACKAGES_FILE = "data/codehelper/packages.xml";
	private static final String DOCCLASSES_FILE = "data/codehelper/docclasses.xml";

	private static PackageParser packageParser;
	private static PackageParser docClassesParser;

	public static void main(String[] args) {
		try {
			long startTime = System.nanoTime();

			packageParser = getPackageParser();
			docClassesParser = getDocClassesParser();

			Package graphics = packageParser.getPackages().get("graphics");
			for (Package dependantPackage : graphics.getDependantPackagesRecursively()) {
				System.out.println(dependantPackage.getName());
			}

			System.out.println((System.nanoTime() - startTime) / (1000 * 1000));
			System.out.println();
			System.out.println(graphics.getDependantPackagesRecursively().size());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static PackageParser getPackageParser() {
		if (packageParser == null) {
			packageParser = new PackageParser(PACKAGES_FILE);

			// long time = System.nanoTime();
			packageParser.finalizePackages();
			// System.out.println((System.nanoTime() - time) / 1000 / 1000);
		}
		return packageParser;
	}

	public static PackageParser getDocClassesParser() {
		if (docClassesParser == null) {
			docClassesParser = new PackageParser(DOCCLASSES_FILE);
		}
		return docClassesParser;
	}

	/**
	 * Test Handler that prints debug statements
	 */
	public static class PackageParser extends DefaultHandler implements ContentHandler {
		private Package pack;
		private String debPack;
		private Trie<Package> packages = new Trie<Package>();
		private TrieSet<Command> commands = new TrieSet<Command>();

		public PackageParser(String fileName) {
			parse(fileName);
		}

		public void parse(String fileName) {
			try {
				XMLReader parser = XMLReaderFactory.createXMLReader();
				parser.setContentHandler(this);
				parser.setErrorHandler(this);
				parser.parse(new InputSource(StreamUtils.getInputStream(fileName)));
			} catch (Exception se) {
				se.printStackTrace();
			}
		}

		public void startElement(String namespaceURI, String localName, String qname, Attributes attrList) {
			//System.out.println("start: " + localName);
			if (localName.equals("command")) {
				String name = attrList.getValue("name");
				int argCount = Integer.parseInt(attrList.getValue("argCount"));
				String optionalArg = attrList.getValue("optionalArg");
				Command command = new Command(name, argCount, optionalArg, pack);
				commands.add(name, command);
			} else
			if (localName.equals("package")) {
				pack = new Package(attrList.getValue("name"), attrList.getValue("options"), attrList.getValue("requiresPackages"), attrList.getValue("title"), attrList.getValue("description"), attrList.getValue("debPackage"));
				packages.add(pack.name, pack);
			}
		}

		@Override
		public void error(SAXParseException e) throws SAXException {
			//System.out.println(e.getLineNumber());
			super.error(e);
		}

		@Override
		public void fatalError(SAXParseException e) throws SAXException {
			//System.out.println(e.getLineNumber());
			super.fatalError(e);
		}

		public Trie<Package> getPackages() {
			return packages;
		}

		public TrieSet<Command> getCommands() {
			return commands;
		}

		protected void finalizePackages() {
			for (Package pack : packages) {
				pack.finalizePackage();
			}
		}
	}

	public static class Package implements Comparable<Package> {
		private String name;
		private String[] options = new String[0];
		private String[] requiresPackageNames = new String[0];
		private ArrayList<Package> requiresPackages = null;
		private ArrayList<Package> dependantPackages = new ArrayList<Package>();
		private String title;
		private String description;
		private String debPackage;

		public Package(String name, String optionsList, String requiresPackagesList, String title, String description, String debPackage) {
			this.name = name;
			if (optionsList != null) {
				options = optionsList.split(",");
			}
			if (requiresPackagesList != null) {
				requiresPackageNames = requiresPackagesList.split(",");
			}
			this.title = title;
			this.description = description;
			this.debPackage = debPackage;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Package) {
				Package that = (Package) obj;
				return this.name.equals(that.name);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		public String getName() {
			return name;
		}

		public String[] getOptions() {
			return options;
		}

		public ArrayList<Package> getRequiresPackages() {
			if (requiresPackages == null) {
				requiresPackages = new ArrayList<Package>();
				for (String packageName : requiresPackageNames) {
					Package pack = getPackageParser().getPackages().get(packageName);
					if (pack != null) {
						requiresPackages.add(pack);
					}
				}
				// free memory
				requiresPackageNames = null;
			}
			return requiresPackages;
		}

		public ArrayList<Package> getDependantPackages() {
			return dependantPackages;
		}

		public HashSet<Package> getDependantPackagesRecursively() {
			return addDependantPackagesRecursively(new HashSet<Package>());
		}

		public HashSet<Package> addDependantPackagesRecursively(HashSet<Package> depPackages) {
			if (!depPackages.contains(this)) {
				depPackages.add(this);
				for (Package dependantPackage : dependantPackages) {
					dependantPackage.addDependantPackagesRecursively(depPackages);
				}
			}
			return depPackages;
		}

		public String getTitle() {
			return title;
		}

		public String getDescription() {
			return description;
		}

		public String getDebPackage() {
			return debPackage;
		}

		@Override
		public int compareTo(Package that) {
			return this.name.compareTo(that.name);
		}

		public void finalizePackage() {
			for (Package pack : getRequiresPackages()) {
				pack.dependantPackages.add(this);
			}
		}
	}

	public static class Command implements Comparable<Command> {
		private String name;
		private int argCount;
		private String optionalArg;
		private Package pack;

		public Command(String name, int argCount, String optionalArg, Package pack) {
			this.name = name;
			this.argCount = argCount;
			this.optionalArg = optionalArg;
			this.pack = pack;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Command) {
				Command that = (Command) obj;
				return this.name.equals(that.name) && this.pack.equals(that.pack);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return name.hashCode() + 37*pack.hashCode();
		}

		public String getName() {
			return name;
		}

		public int getArgCount() {
			return argCount;
		}

		public String getOptionalArg() {
			return optionalArg;
		}

		public Package getPack() {
			return pack;
		}

		@Override
		public int compareTo(Command that) {
			return this.name.compareTo(that.name);
		}
	}
}
