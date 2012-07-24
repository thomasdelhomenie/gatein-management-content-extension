package org.exoplatform.management.content.operations.templates.applications;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Node;

import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Thomas Delhoménie</a>
 * @version $Revision$
 */
public class ApplicationTemplateExportTask implements ExportTask {
	private final ApplicationTemplateManagerService applicationTemplateManagerService;
	private final String applicationName;
	private final String templatePath;
	private final String templateCategory;
	private final String templateName;

	public ApplicationTemplateExportTask(ApplicationTemplateManagerService applicationTemplateManagerService, String applicationName, String templatePath) {
		this.applicationTemplateManagerService = applicationTemplateManagerService;
		this.applicationName = applicationName;
		this.templatePath = templatePath;
		String templatePathArray[] = this.templatePath.split("/");
		if (templatePathArray.length == 1) {
			this.templateCategory = "";
			this.templateName = templatePathArray[0];
		} else {
			this.templateCategory = templatePathArray[0];
			this.templateName = templatePathArray[1];
		}
	}

	@Override
	public String getEntry() {
		return "templates/applications/" + this.applicationName + "/" + templatePath;
	}

	@Override
	public void export(OutputStream outputStream) throws IOException {
		InputStream templateFileIS = null;
		try {
			Node templateNode = applicationTemplateManagerService.getTemplateByName(this.applicationName, this.templateCategory, this.templateName,
					WCMCoreUtils.getSystemSessionProvider());
			
			templateFileIS = templateNode.getNode("jcr:content").getProperty("jcr:data").getStream();
			
			byte[] buffer = new byte[1024];
		    int bytesRead;
		    while ((bytesRead = templateFileIS.read(buffer)) != -1) {
		    	outputStream.write(buffer, 0, bytesRead);
		    }
		} catch (Exception e) {
			throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export template " + this.applicationName + "/" + this.templatePath, e);
		} finally {
			if(templateFileIS != null) {
				templateFileIS.close();
			}
		}
	}

}
