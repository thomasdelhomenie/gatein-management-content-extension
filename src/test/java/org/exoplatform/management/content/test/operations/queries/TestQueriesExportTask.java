package org.exoplatform.management.content.test.operations.queries;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.management.content.operations.queries.QueriesExportTask;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.cms.queries.impl.QueryData;
import org.exoplatform.services.cms.queries.impl.QueryPlugin;
import org.gatein.management.api.operation.model.ExportTask;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
		Configuration configuration = new Configuration();
		
		Assert.assertEquals(new QueriesExportTask(configuration, userId).getEntry(), "queries/users/root-queries-configuration.xml");
		Assert.assertEquals(new QueriesExportTask(configuration, null).getEntry(), "queries/shared-queries-configuration.xml");
	}
}