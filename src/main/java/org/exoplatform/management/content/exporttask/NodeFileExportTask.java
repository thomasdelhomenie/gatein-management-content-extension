package org.exoplatform.management.content.exporttask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Node;

import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Thomas Delhoménie</a>
 * @version $Revision$
 */
public class NodeFileExportTask implements ExportTask {
	private final Node node;
	private final String exportPath;

	public NodeFileExportTask(Node node, String exportPath) {
		this.node = node;
		this.exportPath = exportPath;
	}

	@Override
	public String getEntry() {
		return exportPath;
	}

	@Override
	public void export(OutputStream outputStream) throws IOException {
		InputStream nodeFileIS = null;
		try {
			nodeFileIS = node.getNode("jcr:content").getProperty("jcr:data").getStream();
			
			byte[] buffer = new byte[1024];
		    int bytesRead;
		    while ((bytesRead = nodeFileIS.read(buffer)) != -1) {
		    	outputStream.write(buffer, 0, bytesRead);
		    }
		} catch (Exception e) {
			throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export file of node " + exportPath, e);
		} finally {
			if(nodeFileIS != null) {
				nodeFileIS.close();
			}
		}
	}

}
