package sce.codehelper;

import sce.component.SCEPane;

import java.util.ArrayList;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class CombinedCodeAssistant implements CodeAssistant {
	private ArrayList<CodeAssistant> assistants = new ArrayList<CodeAssistant>();

	@Override
	public boolean assistAt(SCEPane pane) {
		for (CodeAssistant assistant : assistants) {
			if (assistant.assistAt(pane)) return true;
		}
		return false;
	}

	/**
	 * Add code assistant to this code assistant collection.
	 *
	 * @param assistant code assistant to add
	 */
	public void addAssistant(CodeAssistant assistant) {
		assistants.add(assistant);
	}
}
