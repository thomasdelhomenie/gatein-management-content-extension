package org.exoplatform.management.content.operations.queries;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.exoplatform.services.cms.queries.impl.QueryData;
import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class QueriesExportTask implements ExportTask {
  private final List<QueryData> models;

  public QueriesExportTask(List<QueryData> models) {
    this.models = models;
  }

  @Override
  public String getEntry() {
    return "queries/shared-queries.xml";
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    xStream.alias("queries", List.class);
    xStream.alias("query", QueryData.class);
    String xmlContent = xStream.toXML(models);
    outputStream.write(xmlContent.getBytes());
  }
}