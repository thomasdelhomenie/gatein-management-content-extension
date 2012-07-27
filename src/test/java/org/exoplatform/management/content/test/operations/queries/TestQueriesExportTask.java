package org.exoplatform.management.content.test.operations.queries;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.exoplatform.management.content.operations.queries.QueriesExportTask;
import org.exoplatform.services.cms.queries.impl.QueryData;
import org.gatein.management.api.operation.model.ExportTask;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;


/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class TestQueriesExportTask {
	
	@Before
	public void setup() {
		
	}

	@Test
	public void exportEntry() {
		String userId = "root";
		List<QueryData> queries = new ArrayList<QueryData>();
		
		Assert.assertEquals(new QueriesExportTask(queries, userId).getEntry(), "queries/users/root-queries.xml");
		Assert.assertEquals(new QueriesExportTask(queries, null).getEntry(), "queries/shared-queries.xml");
	}
	
	@Test
	public void exportUserQueries() throws IOException, SAXException {
		// init data
		String userId = "root";
		List<QueryData> queries = new ArrayList<QueryData>();
		QueryData query = new QueryData();
		query.setName("test");
		query.setLanguage("fr");
		query.setStatement("statement");
		query.setCacheResult(true);
		query.setPermissions(Collections.<String>emptyList());		
		queries.add(query);
		
		// export
		ExportTask exportTask = new QueriesExportTask(queries, userId);		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		exportTask.export(output);
		
		// test XML output
		String controlXML = new StringBuilder()
			.append("<queries>")
			.append("<query>")
			.append("<name>test</name>")
			.append("<language>fr</language>")
			.append("<statement>statement</statement>")
			.append("<permissions/>")
			.append("<cachedResult>true</cachedResult>")
			.append("</query>")
			.append("</queries>")
			.toString();
		
		XMLUnit.setIgnoreWhitespace(true);
		Diff diff = new Diff(controlXML, output.toString("UTF-8"));
		Assert.assertTrue(diff.identical());
	}
}