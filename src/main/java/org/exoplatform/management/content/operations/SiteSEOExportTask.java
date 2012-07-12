package org.exoplatform.management.content.operations;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.exoplatform.services.seo.PageMetadataModel;
import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SiteSEOExportTask implements ExportTask {
  private final List<PageMetadataModel> models;

  public SiteSEOExportTask(List<PageMetadataModel> models) {
    this.models = models;
  }

  @Override
  public String getEntry() {
    return "seo.xml";
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    xStream.alias("seo", List.class);
    String xmlContent = xStream.toXML(models);
    outputStream.write(xmlContent.getBytes());
  }
}