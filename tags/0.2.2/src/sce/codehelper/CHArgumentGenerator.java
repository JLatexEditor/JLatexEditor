package sce.codehelper;

import util.Function1;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CHArgumentGenerator {
	private String argumentName;
	private CHCommandArgument argument;
	private Function1<String, String> function;

	public CHArgumentGenerator(String argumentName, Function1<String, String> function) {
		this.argumentName = argumentName;
		this.function = function;
	}

	public void setArgument(CHCommandArgument argument) {
		this.argument = argument;
	}

	public String getArgumentName() {
		return argumentName;
	}

	public CHCommandArgument getArgument() {
		return argument;
	}

	public Function1<String, String> getFunction() {
		return function;
	}
}
