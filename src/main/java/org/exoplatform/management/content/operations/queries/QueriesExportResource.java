package org.exoplatform.management.content.operations.queries;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;

import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.cms.queries.impl.QueryData;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class QueriesExportResource implements OperationHandler {

	@Override
	public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
		try {
			List<QueryData> queries = new ArrayList<QueryData>();

			QueryService queryService = operationContext.getRuntimeContext().getRuntimeComponent(QueryService.class);
			
			List<Node> sharedQueries = queryService.getSharedQueries(WCMCoreUtils.getSystemSessionProvider());
			
			// Queries API returns Node object instead of QueryData, so we need to convert them...
			for(Node sharedQueryNode : sharedQueries) {
				QueryData queryData = new QueryData();
				queryData.setName(sharedQueryNode.getProperty("exo:name").getString());
				queryData.setStatement(sharedQueryNode.getProperty("jcr:statement").getString());
				queryData.setLanguage(sharedQueryNode.getProperty("jcr:language").getString());
				queryData.setCacheResult(sharedQueryNode.getProperty("exo:cachedResult").getBoolean());
				Value[] permissionsValues = sharedQueryNode.getProperty("exo:accessPermissions").getValues();
				List<String> permissions = new ArrayList<String>();
				for(Value permissionValue : permissionsValues) {
					permissions.add(permissionValue.getString());
				}
				queryData.setPermissions(permissions);
				
				queries.add(queryData);
			}

			
			resultHandler.completed(new ExportResourceModel(new QueriesExportTask(queries)));
		} catch (Exception e) {
			throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to retrieve the list of the contents sites : " + e.getMessage());
		}
	}
}
