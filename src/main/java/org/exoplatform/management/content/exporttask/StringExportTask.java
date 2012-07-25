package org.exoplatform.management.content.exporttask;

import java.io.IOException;
import java.io.OutputStream;

import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Thomas Delhoménie</a>
 * @version $Revision$
 */
public class StringExportTask implements ExportTask {
	private final String content;
	private final String exportPath;

	public StringExportTask(String content, String exportPath) {
		this.content = content;
		this.exportPath = exportPath;
	}

	@Override
	public String getEntry() {
		return exportPath;
	}

	@Override
	public void export(OutputStream outputStream) throws IOException {
		try {
			outputStream.write(content.getBytes());
		} catch (Exception e) {
			throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export file of node " + exportPath, e);
		}
	}

}
