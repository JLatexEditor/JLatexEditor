package sce.codehelper;

import util.Function1;

/**
 * Argument generation.
 *
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
		return argument != null ? argument.getName() : argumentName;
	}

	public CHCommandArgument getArgument() {
		return argument;
	}

	public Function1<String, String> getFunction() {
		return function;
	}

	public void setFunction(Function1<String, String> function) {
		this.function = function;
	}

	public String getFunctionName() {
		return CHFunctions.getName(function);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CHArgumentGenerator) {
			CHArgumentGenerator that = (CHArgumentGenerator) obj;
			return this.argument.getName().equals(that.argument.getName()) && this.function.equals(that.function);
		} else {
			return false;
		}
	}
}
