package org.exoplatform.management.content.test.operations.queries;

import org.exoplatform.container.xml.Configuration;
import org.exoplatform.management.content.operations.queries.QueriesExportTask;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


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