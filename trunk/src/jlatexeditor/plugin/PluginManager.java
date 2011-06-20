package jlatexeditor.plugin;

import jlatexeditor.addon.AddOn;
import net.sf.extcos.ComponentQuery;
import net.sf.extcos.ComponentScanner;

import java.util.Set;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class PluginManager {
	public static void main(String[] args) {
		ComponentScanner scanner = new ComponentScanner();
		Set<Class<?>> classes = scanner.getClasses(new ComponentQuery() {
			protected void query() {
				select().from("jlatexeditor.addon", "addon").returning(allExtending(AddOn.class));
			}
		});

		for (Class<?> aClass : classes) {
			System.out.println(aClass.getName());
		}
	}
}
