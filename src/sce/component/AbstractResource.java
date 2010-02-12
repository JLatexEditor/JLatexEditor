package sce.component;

import java.io.IOException;
import java.net.URI;

/**
 * Abstract document
 */
public interface AbstractResource {
	public String getName();
	public URI getUri();
	public String getContent() throws IOException;
}
