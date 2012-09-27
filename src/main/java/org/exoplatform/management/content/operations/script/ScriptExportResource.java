package org.exoplatform.management.content.operations.script;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
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
public class ScriptExportResource implements OperationHandler {

  private ScriptService scriptService = null;

  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (scriptService == null) {
      scriptService = operationContext.getRuntimeContext().getRuntimeComponent(ScriptService.class);
    }
    List<ExportTask> exportTasks = new ArrayList<ExportTask>();
    try {
      // Add script definition into the InitParams of the Component
      SessionProvider systemSessionProvider = SessionProvider.createSystemProvider();

      List<Node> ecmActionScripts = scriptService.getECMActionScripts(systemSessionProvider);
      generateScriptsConfiguration(exportTasks, ecmActionScripts);

      List<Node> ecmInterceptorScripts = scriptService.getECMInterceptorScripts(systemSessionProvider);
      generateScriptsConfiguration(exportTasks, ecmInterceptorScripts);

      List<Node> ecmWidgetScripts = scriptService.getECMWidgetScripts(systemSessionProvider);
      generateScriptsConfiguration(exportTasks, ecmWidgetScripts);
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while retrieving script", exception);
    }
    resultHandler.completed(new ExportResourceModel(exportTasks));
  }

  private void generateScriptsConfiguration(List<ExportTask> exportTasks, List<Node> nodes) throws Exception {
    for (Node node : nodes) {
      String scriptPath = node.getPath().replace("/exo:ecm/scripts/", "");
      String scriptData = node.getNode("jcr:content").getProperty("jcr:data").getString();
      exportTasks.add(new ScriptExportTask(scriptPath, scriptData));
    }
  }
}