package org.exoplatform.management.content.operations.nodetype;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class NodeTypeExportResource implements OperationHandler {

  private RepositoryService repositoryService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (repositoryService == null) {
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    }
    List<ExportTask> exportTasks = new ArrayList<ExportTask>();
    try {
      NodeTypeManager ntManager = repositoryService.getCurrentRepository().getNodeTypeManager();
      NodeTypeIterator nodeTypesIterator = ntManager.getAllNodeTypes();
      while (nodeTypesIterator.hasNext()) {
        NodeType nodeType = nodeTypesIterator.nextNodeType();
        exportTasks.add(new NodeTypeExportTask(nodeType));
      }
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while retrieving nodetypes", exception);
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

}