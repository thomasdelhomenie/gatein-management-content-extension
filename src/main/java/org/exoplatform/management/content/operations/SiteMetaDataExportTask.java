package org.exoplatform.management.content.operations;

import java.io.IOException;
import java.io.OutputStream;

import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SiteMetaDataExportTask implements ExportTask {

  private SiteMetaData metaData = null;

  public SiteMetaDataExportTask(SiteMetaData metaData) {
    this.metaData = metaData;
  }

  @Override
  public String getEntry() {
    return "metadata.xml";
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    xStream.alias("metadata", SiteMetaData.class);
    String xmlContent = xStream.toXML(metaData);
    outputStream.write(xmlContent.getBytes());
  }
}