package org.exoplatform.management.content.operations.templates.metadata;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.management.content.exporttask.StringExportTask;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas Delhoménie</a>
 * @version $Revision$
 */
public class MetadataTemplatesExportResource implements OperationHandler {
	
	private static final String EXPORT_BASE_PATH = "templates/metadata";
	
	private MetadataService metadataService;
	private NodeHierarchyCreator nodeHierarchyCreator;
	
	@Override
	public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
		
		String operationName = operationContext.getOperationName();

		List<ExportTask> exportTasks = new ArrayList<ExportTask>();

		metadataService = operationContext.getRuntimeContext().getRuntimeComponent(MetadataService.class);
		nodeHierarchyCreator = operationContext.getRuntimeContext().getRuntimeComponent(NodeHierarchyCreator.class);

		try {
			// MetadataService does not expose the templates base node path...
			String templatesBasePath = nodeHierarchyCreator.getJcrPath(BasePath.METADATA_PATH);
			
			boolean[] isDialogValues = new boolean[]{true, false};
			List<String> metadataList = metadataService.getMetadataList();
			for(String metadataName : metadataList) {	
				for(boolean isDialog : isDialogValues) {
					String metadataPath = metadataService.getMetadataPath(metadataName, isDialog);
					String metadataRoles = metadataService.getMetadataRoles(metadataName, isDialog);
					String metadataTemplate = metadataService.getMetadataTemplate(metadataName, isDialog);
					
					exportTasks.add(new StringExportTask(metadataTemplate, EXPORT_BASE_PATH + metadataPath.substring(templatesBasePath.length()) + ".gtmpl"));
				}
			}			
		} catch (Exception e) {
			throw new OperationException(operationName, "Error while retrieving node types templates", e);
		}
		
		resultHandler.completed(new ExportResourceModel(exportTasks));
	}
}