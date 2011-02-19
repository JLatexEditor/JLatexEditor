package jlatexeditor.addon;

import jlatexeditor.JLatexEditorJFrame;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Add On.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public abstract class AddOn {
// static
	private static LinkedHashMap<String,AddOn> allAddOnsMap = new LinkedHashMap<String, AddOn>();

	static {
		AddOn[] addOns = {
			new RenameElement(),
			new CloseEnvironment(),
			new ColumnRealigner()
		};

		for (AddOn addOn : addOns) {
			allAddOnsMap.put(addOn.getKey(), addOn);
		}
	}

	public static LinkedHashMap<String,AddOn> getAllAddOnsMap() {
		return allAddOnsMap;
	}


// dynamic
	private String key;
	private String label;
	private String shortcut;
	private String comment;

	protected AddOn(String key, String label, String shortcut) {
		this.key = key;
		this.label = label;
		this.shortcut = shortcut;
		this.comment = "Shortcut for " + label;
	}

	protected AddOn(String key, String label, String shortcut, String comment) {
		this.key = key;
		this.label = label;
		this.shortcut = shortcut;
		this.comment = comment;
	}

	public String getKey() {
		return key;
	}

	public String getLabel() {
		return label;
	}

	public String getShortcut() {
		return shortcut;
	}

	public String getComment() {
		return comment;
	}

	public abstract void run(JLatexEditorJFrame jle);
}
