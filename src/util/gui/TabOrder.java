package util.gui;

import java.awt.*;
import java.util.*;

/**
 * Custom tab order.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class TabOrder extends FocusTraversalPolicy {
	java.util.List<Component> order;

	public TabOrder(Component... order) {
		this.order = Arrays.asList(order);
	}

	public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {
		int idx = (order.indexOf(aComponent) + 1) % order.size();
		return order.get(idx);
	}

	public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {
		int idx = order.indexOf(aComponent) - 1;
		if (idx < 0) {
			idx = order.size() - 1;
		}
		return order.get(idx);
	}

	public Component getDefaultComponent(Container focusCycleRoot) {
		return order.get(0);
	}

	public Component getLastComponent(Container focusCycleRoot) {
		return order.get(order.size() - 1);
	}

	public Component getFirstComponent(Container focusCycleRoot) {
		return order.get(0);
	}
}
