package jlatexeditor;

import sce.component.AbstractResource;

import java.io.IOException;
import java.net.URI;

public class EmptyResource implements AbstractResource {
  public String getName() {
    return "Empty Resource";
  }

  public URI getUri() {
    return null;
  }

  public String getContent() throws IOException {
    return "";
  }
}
