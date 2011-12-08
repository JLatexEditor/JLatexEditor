package sce.component;

import javax.swing.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import sce.component.SCEPaneUI.Actions;

/**
 * Source code editor UI.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class SourceCodeEditorUI {
	public static enum Comp { global, textPane, search }
	
	public static final HashSet<String> globalActions = new HashSet<String>(){{
		add(Actions.FIND);
		add(Actions.REPLACE);
		add(Actions.FIND_NEXT);
		add(Actions.FIND_PREVIOUS);
	}};

	private static final ArrayList<WeakReference<SourceCodeEditor>> sces = new ArrayList<WeakReference<SourceCodeEditor>>();
	private static final HashMap<Comp, HashMap<KeyStroke, String>> keyStrokeMaps = new HashMap<Comp, HashMap<KeyStroke, String>>(){{
		for (Comp comp : Comp.values()) {
			put(comp, new HashMap<KeyStroke, String>());
		}
	}};
	private static final HashMap<String, KeyStroke> actionMap = new HashMap<String, KeyStroke>();

	protected static void installInputMap(SourceCodeEditor sce) {
		sces.add(new WeakReference<SourceCodeEditor>(sce));

		for (Map.Entry<KeyStroke, String> keyStrokeStringEntry : keyStrokeMaps.get(Comp.global).entrySet()) {
			sce.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStrokeStringEntry.getKey(), keyStrokeStringEntry.getValue());
		}
		for (Map.Entry<KeyStroke, String> keyStrokeStringEntry : keyStrokeMaps.get(Comp.textPane).entrySet()) {
			sce.getTextPane().getInputMap().put(keyStrokeStringEntry.getKey(), keyStrokeStringEntry.getValue());
		}
	}

	protected static void uninstallInputMap(SourceCodeEditor sce) {
		sces.remove(new WeakReference<SourceCodeEditor>(sce));

		for (Map.Entry<KeyStroke, String> keyStrokeStringEntry : keyStrokeMaps.get(Comp.global).entrySet()) {
			sce.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).remove(keyStrokeStringEntry.getKey());
		}
		for (Map.Entry<KeyStroke, String> keyStrokeStringEntry : keyStrokeMaps.get(Comp.textPane).entrySet()) {
			sce.getTextPane().getInputMap().remove(keyStrokeStringEntry.getKey());
		}
	}

	public static void replaceKeyStrokeAndAction(Comp comp, KeyStroke keyStroke, String action) {
		// remove action
		if (actionMap.containsKey(action)) {
			removeKeyStroke(actionMap.get(action));
		}
		// remove keystroke
		if (keyStrokeMaps.get(comp).containsKey(keyStroke)) {
			removeKeyStroke(keyStroke);
		}
		// add keystroke
		addKeyStroke(comp, keyStroke, action);
	}

	public static void addKeyStroke(Comp comp, KeyStroke keyStroke, String action) {
		// overwrite keystroke
		for (WeakReference<SourceCodeEditor> sceRef : sces) {
			SourceCodeEditor sce = sceRef.get();
			if (sce != null) {
				getInputMap(comp, sce).put(keyStroke, action);
			}
		}
		keyStrokeMaps.get(comp).put(keyStroke, action);
		actionMap.put(action, keyStroke);
	}

	public static void removeKeyStroke(KeyStroke keyStroke) {
		for (Comp comp : Comp.values()) {
			HashMap<KeyStroke, String> keyStrokeMap = keyStrokeMaps.get(comp);
			if (keyStrokeMap.containsKey(keyStroke)) {
				for (WeakReference<SourceCodeEditor> paneRef : sces) {
					SourceCodeEditor sce = paneRef.get();
					if (sce != null) {
						getInputMap(comp, sce).remove(keyStroke);
					}
				}
			}
		}

		// remove actions for 
		for (Comp comp : Comp.values()) {
			actionMap.remove(keyStrokeMaps.get(comp).get(keyStroke));
			keyStrokeMaps.get(comp).remove(keyStroke);
		}
	}
	
	private static InputMap getInputMap(Comp comp, SourceCodeEditor sce) {
		switch (comp) {
			case global:
				return sce.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
			case textPane:
				return sce.getTextPane().getInputMap();
			default:
				return null;
		}
	}
}
