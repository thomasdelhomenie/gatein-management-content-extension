package org.exoplatform.management.content.operations.site.taxonomy;

import java.io.IOException;
import java.io.OutputStream;

import org.exoplatform.management.content.operations.templates.NodeTemplate;
import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class TaxonomyMetaDataExportTask implements ExportTask {

  private TaxonomyMetaData metaData = null;
  private String path = null;

  public TaxonomyMetaDataExportTask(TaxonomyMetaData metaData, String path) {
    this.metaData = metaData;
    this.path = path;
  }

  @Override
  public String getEntry() {
    return path + "/metadata.xml";
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    xStream.alias("metadata", TaxonomyMetaData.class);
    xStream.alias("taxonomy", NodeTemplate.class);
    String xmlContent = xStream.toXML(metaData);
    outputStream.write(xmlContent.getBytes());
  }
}