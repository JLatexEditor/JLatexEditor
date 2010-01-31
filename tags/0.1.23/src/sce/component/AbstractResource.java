package sce.component;

import java.io.IOException;

/**
 * Abstract document
 */
public interface AbstractResource {
	public String getName();
	public String getContent() throws IOException;
}
