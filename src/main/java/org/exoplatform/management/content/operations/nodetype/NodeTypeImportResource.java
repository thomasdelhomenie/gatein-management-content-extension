package org.exoplatform.management.content.operations.nodetype;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipInputStream;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValuesList;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class NodeTypeImportResource implements OperationHandler {
  private RepositoryService repositoryService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (repositoryService == null) {
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    }
    try {
      ExtendedNodeTypeManager extManager = (ExtendedNodeTypeManager) repositoryService.getCurrentRepository()
          .getNodeTypeManager();
      OperationAttachment attachment = operationContext.getAttachment(false);
      InputStream attachmentInputStream = attachment.getStream();
      if (attachmentInputStream == null) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for nodetypes.");
      }
      ZipInputStream zin = new ZipInputStream(attachmentInputStream);
      while (zin.getNextEntry() != null) {
        IBindingFactory factory = BindingDirectory.getFactory(NodeTypeValuesList.class);
        IUnmarshallingContext uctx = factory.createUnmarshallingContext();
        NodeTypeValuesList nodeTypeValuesList = (NodeTypeValuesList) uctx.unmarshalDocument(zin, null);
        ArrayList<?> ntvList = nodeTypeValuesList.getNodeTypeValuesList();
        for (Object object : ntvList) {
          NodeTypeValue nodeTypeValue = (NodeTypeValue) object;
          extManager.registerNodeType(nodeTypeValue, ExtendedNodeTypeManager.IGNORE_IF_EXISTS);
        }
        zin.closeEntry();
      }
      zin.close();
      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Exception exception) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing nodetypes.", exception);
    }
  }

}
