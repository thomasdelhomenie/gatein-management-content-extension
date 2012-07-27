package org.exoplatform.management.content.operations.queries;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.exoplatform.services.cms.queries.impl.QueryData;
import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class QueriesExportTask implements ExportTask {
	private final List<QueryData> models;
	private final String userId;

	public QueriesExportTask(List<QueryData> models, String userId) {
		this.models = models;
		this.userId = userId;
	}

	@Override
	public String getEntry() {
		if (userId != null) {
			return "queries/users/" + this.userId + "-queries.xml";
		} else {
			return "queries/shared-queries.xml";
		}
	}

	@Override
	public void export(OutputStream outputStream) throws IOException {
		XStream xStream = new XStream();
		xStream.alias("queries", List.class, ArrayList.class);
		xStream.alias("query", QueryData.class);

		// Overwrites the addAttribute method to remove the 'class'
		// attribute, so a empty permissions list tag will be clean.
		// TODO see if this does not break the unmarshalling
		StringWriter writer = new StringWriter();
		xStream.marshal(models, new PrettyPrintWriter(writer) {
	        @Override
	        public void addAttribute(final String key, final String value)
	        {
	            if (!key.equals("class"))
	            {
	                super.addAttribute(key, value);
	            }
	        }
	    });
		outputStream.write(writer.toString().getBytes());
	}
}