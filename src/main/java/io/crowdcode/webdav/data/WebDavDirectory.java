package io.crowdcode.webdav.data;

import java.net.URI;
import org.apache.jackrabbit.webdav.property.DavPropertySet;

/**
 * WebDAV directory representation
 */
public final class WebDavDirectory implements WebDavElement {

  private final URI baseURI;
  private final DavPropertySet propertiesPresent;

  public WebDavDirectory(URI baseURI, DavPropertySet propertiesPresent) {
    this.baseURI = baseURI;
    this.propertiesPresent = propertiesPresent;
  }

  public URI getBaseURI() {
    return baseURI;
  }

  @Override
  public DavPropertySet getPropertiesPresent() {
    return propertiesPresent;
  }
}
