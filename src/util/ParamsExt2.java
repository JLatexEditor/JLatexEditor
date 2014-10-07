package util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Extended command line parameter manager.
 * Manages command line parameters in the form "-k", "--key",
 * "-k [value1 [value2 [...]]]" or "--key[=value1[:value2[:...]]]".
 *
 * @author Stefan Endrullis
 * @version 1.0
 */
public abstract class ParamsExt2 {
	/** Syntax. */
	protected String syntax = null;
	/** Maps the option name to the Option object. */
	protected Map<String,Option> optionByName = new HashMap<String, Option>();
	/** Maps the option mnemonic to the Option object. */
	protected Map<Character,Option> optionByMnemonic = new HashMap<Character, Option>();
	/** Command line arguments which are not bound to an option name. */
	protected ArrayList<String> unboundArguments = new ArrayList<String>();
	/** Maximal length of option names. */
	protected int maxOptionNameLength = 0;

	public void init(String[] args) {
		// clear previews values
		unboundArguments.clear();
		for (Option option : optionByName.values()) {
			option.values.clear();
		}

		// determine maximal length of option names
		for (Option option : optionByName.values()) {
		  maxOptionNameLength = Math.max(maxOptionNameLength, option.name.length());
		}

		// process args
		// parse options
		for (int i = 0; i < args.length; i++) {
		  String argument = args[i];

		  if(argument.startsWith("--")) {
		    int eqIndex = argument.indexOf('=');
		    if(eqIndex != -1){
		      String[] values = argument.substring(eqIndex + 1).split(":");
			    Option option = optionByName.get(argument.substring(2, eqIndex));
			    option.values.addAll(Arrays.asList(values));
			    option.set = true;
		    }else{
			    Option option = optionByName.get(argument.substring(2));
			    option.set = true;
		    }
		  }
		  else if(argument.startsWith("-")) {
		    Option option = optionByMnemonic.get(argument.charAt(1));
			  option.set = true;

		    if(option.paramsCount > 0) {
		      String values[] = new String[option.paramsCount];
		      try {
		        System.arraycopy(args, i + 1, values, 0, values.length);
		        i += values.length;
		      } catch(ArrayIndexOutOfBoundsException e) {
		        System.err.println("option -" + option.mnemonic + " requires more arguments");
		        System.exit(1);
		      }
			    option.values.addAll(Arrays.asList(values));
		    }
		  }
		  else {
		    unboundArguments.add(argument);
		  }
		}
	}

	public String bold(String text) {
		return Shell.bold(text);
	}
	public String underline(String text) {
		return Shell.underline(text);
	}

	public void printHelp() {
		printHelp(System.out);
	}

	public void printHelp(PrintStream out) {
	  if(syntax != null) {
	    out.println(bold("SYNTAX"));
	    out.println("  " + syntax);
	    out.println();
	  }

	  out.println(bold("OPTIONS"));
	  for (Option option : optionByName.values()) {
	    StringBuilder sb = new StringBuilder();
	    if(option.mnemonic == null) {
	      sb.append("     ");
	    } else {
	      sb.append(bold("  -" + option.mnemonic)).append(",");
	    }
	    sb.append(bold("--" + option.name));
	    appendSpaces(sb, 3 + maxOptionNameLength - option.name.length());
	    sb.append(option.description);

	    out.println(sb.toString());
	  }
	}

	private void appendSpaces(StringBuilder sb, int count) {
	  for(int i = 0; i < count; i++) {
	    sb.append(' ');
	  }
	}

	public void syntax(String syntax) {
		this.syntax = syntax;
	}

	/** Defines a new option. */
	public Option option(String name, Character mnemonic, String description) {
		return option(name, mnemonic, description, 0);
	}
	/** Defines a new option. */
	public Option option(String name, Character mnemonic, String description, int paramsCount) {
		Option option = new Option(name, mnemonic, description, paramsCount);
		if (name != null) {
			optionByName.put(name, option);
		}
		if (mnemonic != 0) {
			optionByMnemonic.put(mnemonic, option);
		}
		return option;
	}

	public Map<String, Option> getOptionByName() {
		return optionByName;
	}

	public ArrayList<String> getUnboundArguments() {
		return unboundArguments;
	}

	/**
	 * This is the option structure without logic.
	 */
	public static class Option {
	  private String name;
	  private Character mnemonic;
	  private String description;
	  private int paramsCount = 0;
		protected boolean set = false;
		protected ArrayList<String> values = new ArrayList<String>();

	  public Option(String name, Character mnemonic, String description, int paramsCount) {
	    this.name = name;
	    this.mnemonic = mnemonic;
	    this.description = description;
	    this.paramsCount = paramsCount;
	  }

		public String getName() {
			return name;
		}

		public Character getMnemonic() {
			return mnemonic;
		}

		public String getDescription() {
			return description;
		}

		public int getParamsCount() {
			return paramsCount;
		}

		public boolean isSet() {
			return set;
		}

		public ArrayList<String> getValues() {
			return values;
		}

		public String getValue() {
		  if (values.size() == 0) return null;
			else return values.get(0);
		}
	}
}
