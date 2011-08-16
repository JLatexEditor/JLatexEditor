package jlatexeditor.codehelper;

import sce.codehelper.CHCommand;
import sce.codehelper.CHCommandArgument;

/**
 * Environment defined via \newenvironment.
 */
public class Environment extends BackgroundParser.FilePos {
  private String name;
  private int numberOfArgs;
  private String optional;

	public Environment(String name, String file, int lineNr, int numberOfArgs, String optional) {
		super(name, file, lineNr);
		this.name = name;
		this.numberOfArgs = numberOfArgs;
		this.optional = optional;
	}

	public String getName() {
    return name;
  }

  public int getNumberOfArgs() {
    return numberOfArgs;
  }

  public String getOptional() {
    return optional;
  }

  public String toString() {
    return "\\newcommand{\\" + name + "}" +
            (numberOfArgs > 0 ? "[" + numberOfArgs + "]" : "") +
            (optional != null ? "[" + optional + "]" : "");
  }

	public CHCommand toCHCommand() {
		CHCommand chCommand = new CHCommand("\\" + name);
		String template = "\\" + name;
		if (optional != null) {
			template += "[@opt@]";
			chCommand.addArgument(new CHCommandArgument("opt", optional, true));
		}
		for (int i=1; i<=numberOfArgs; i++) {
			template += "{@arg" + i + "@}";
			chCommand.addArgument(new CHCommandArgument("arg" + i, "", false));
		}
		chCommand.setUsage(template);
		return chCommand;
	}
}
