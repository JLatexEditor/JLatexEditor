package sce.component;

/**
 * Range in a SECDocument.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public interface SCERange {
	public int getStartRow();
	public int getStartCol();
	public int getEndRow();
	public int getEndCol();
}
