package org.exoplatform.management.content.operations;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SiteContentExportTask implements ExportTask {
  private final RepositoryService repositoryService;
  private final String workspace;
  private final String absolutePath;

  public SiteContentExportTask(RepositoryService repositoryService, String workspace, String absolutePath) {
    this.repositoryService = repositoryService;
    this.workspace = workspace;
    this.absolutePath = absolutePath;
  }

  @Override
  public String getEntry() {
    return "contents-sysview" + absolutePath + ".xml";
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    Session session = null;
    try {
      session = repositoryService.getCurrentRepository().getSystemSession(workspace);
      session.exportSystemView(absolutePath, outputStream, false, false);
    } catch (RepositoryException exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export content from : " + absolutePath, exception);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

}
