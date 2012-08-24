/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.management.content.operations.site.contents;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.management.content.operations.site.contents.SiteMetaData;
import org.exoplatform.management.content.operations.site.contents.SiteMetaDataExportTask;
import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:soren.schmidt@exoplatform.com">Soren Schmidt</a>
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas Delhoménie</a>
 * @version $Revision$
 * 
 *          usage: ssh -p 2000 john@localhost mgmt connect ls cd content import -f /acmeTest.zip
 * 
 */
public class SiteContentsImportResource implements OperationHandler {

	final private static Logger log = LoggerFactory.getLogger(SiteContentsImportResource.class);

	private String operationName = null;

	@Override
	public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {

		operationName = operationContext.getOperationName();

		// "uuidBehavior" attribute
		int uuidBehaviorValue = extractUuidBehavior(operationContext.getAttributes().getValue("uuidBehavior"));

		Map<String, String> nodes = new HashMap<String, String>();

		SiteMetaData metaData = collectFileData(operationContext, nodes);

		Map<String, String> metaDataOptions = metaData.getOptions();
		String workspace = metaDataOptions.get("site-workspace");
		String siteName = metaDataOptions.get("site-name"); // never used
		String sitePath = metaDataOptions.get("site-path"); // never used
		log.info("Reading metadata options for import: workspace: " + workspace);

		try {
			importContentNodes(operationContext, metaData, nodes, workspace, uuidBehaviorValue);
			log.info("Content import has been finished");
			resultHandler.completed(NoResultModel.INSTANCE);
		} catch (Exception e) {
			throw new OperationException(operationName, "Unable to create import task", e);
		}
	}

	private void importContentNodes(OperationContext operationContext, SiteMetaData metaData, Map<String, String> nodes, String workspace,
			int uuidBehaviorValue) throws Exception {

		RepositoryService repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
		Session session = repositoryService.getCurrentRepository().getSystemSession(workspace);

		for (Iterator<String> it = nodes.keySet().iterator(); it.hasNext();) {
			String name = (String) it.next();
			String path = metaData.getExportedFiles().get(name);

			String relPath = path + name.substring(name.lastIndexOf("/"), name.lastIndexOf('.'));

			if (log.isInfoEnabled()) {
				log.info("Deleting the node " + workspace + ":" + relPath);
			}

			if (relPath.startsWith("/")) {
				relPath = relPath.substring(1);
			}

			try {
				Node oldNode = session.getRootNode().getNode(relPath);
				oldNode.remove();
			} catch (PathNotFoundException e) {
				log.error("Error when trying to find and delete the node: " + relPath, e);
			} catch (RepositoryException e) {
				log.error("Error when trying to find and delete the node: " + relPath, e);
			}

			if (log.isInfoEnabled()) {
				log.info("Importing the node " + name + " to the node " + path);
			}

			// Create the parent path
			createJCRPath(session, path);

			session.importXML(path, new ByteArrayInputStream(nodes.get(name).getBytes("UTF-8")), uuidBehaviorValue);
		}
		// save at the end
		// TODO Can there be too much data?
		// TODO Transaction instead of a simple session?
		session.save();

	}

	private void createJCRPath(Session session, String path) throws RepositoryException {

		String[] ancestors = path.split("/");
		Node current = session.getRootNode();
		for (int i = 0; i < ancestors.length; i++) {
			if (!"".equals(ancestors[i])) {
				if (current.hasNode(ancestors[i])) {
					current = current.getNode(ancestors[i]);
				} else {
					if(log.isInfoEnabled()) {
						log.info("Creating folder: " + ancestors[i] + " in node : " + current.getPath());
					}
					current = current.addNode(ancestors[i], "nt:unstructured");
				}
			}
		}

	}

	/**
	 * JCR Import UUID Behavior enum
	 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas Delhoménie</a>
	 *
	 */
	private enum ImportBehavior {
		THROW(ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW), //
		REMOVE(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING), //
		REPLACE(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING), //
		NEW(ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);

		final private int behavior;

		ImportBehavior(int behavior) {
			this.behavior = behavior;
		}

		public int getBehavior() {
			return this.behavior;
		}
	}

	/**
	 * Convert UUID behavior string into int
	 * @param uuidBehavior
	 * @return
	 */
	private int extractUuidBehavior(String uuidBehavior) {

		int uuidBehaviorValue;
		if (!StringUtils.isEmpty(uuidBehavior)) {
			try {
				uuidBehaviorValue = ImportBehavior.valueOf(uuidBehavior).getBehavior();
			} catch (Exception e) {
				throw new OperationException(this.operationName, "Unknown uuidBehavior " + uuidBehavior);
			}
		} else {
			uuidBehaviorValue = ImportBehavior.NEW.getBehavior();
		}

		return uuidBehaviorValue;
	}

	/**
	 * Extract metadata file and JCR export XML files form the archive
	 * @param operationContext
	 * @param nodes
	 * @return
	 * @throws OperationException
	 */
	public SiteMetaData collectFileData(OperationContext operationContext, Map<String, String> nodes) throws OperationException {
		OperationAttachment attachment = operationContext.getAttachment(false);
		if (attachment == null) {
			throw new OperationException(this.operationName, "No attachment available for Site Content import.");
		}

		InputStream metaInputStream = attachment.getStream();
		if (metaInputStream == null) {
			throw new OperationException(this.operationName, "No data stream available for Site Content import.");
		}

		final NonCloseableZipInputStream zis = new NonCloseableZipInputStream(metaInputStream);

		SiteMetaData siteMetaData = null;
		try {

			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				// Skip directories
				if (entry.isDirectory())
					continue;
				// Skip empty entries (this allows empty zip files to not cause exceptions).
				if (entry.getName().equals(""))
					continue;

				String file = entry.getName();
				if (file.equals(SiteMetaDataExportTask.FILENAME)) {
					XStream xstream = new XStream();
					xstream.alias("metadata", SiteMetaData.class);
					InputStreamReader isr = new InputStreamReader(zis, "UTF-8");
					siteMetaData = (SiteMetaData) xstream.fromXML(isr);
				}
				if (file.startsWith("sites/contents-sysview")) {
					String name = entry.getName();
					log.info("Collecting the node " + name);
					String nodeContent = convertStreamToString(zis);
					nodes.put(name, nodeContent);
				}
			}

			zis.reallyClose();
		} catch (IOException e) {
			throw new OperationException(this.operationName, "Exception when reading the underlying data stream from import.", e);
		}
		return siteMetaData;
	}

	// Bug in SUN's JDK XMLStreamReader implementation closes the underlying
	// stream when
	// it finishes reading an XML document. This is no good when we are using a
	// ZipInputStream.
	// See http://bugs.sun.com/view_bug.do?bug_id=6539065 for more information.
	private static class NonCloseableZipInputStream extends ZipInputStream {
		private NonCloseableZipInputStream(InputStream inputStream) {
			super(inputStream);
		}

		@Override
		public void close() throws IOException {
		}

		private void reallyClose() throws IOException {
			super.close();
		}
	}

	public String convertStreamToString(InputStream is) throws IOException {

		if (is != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
			}
			return writer.toString();
		} else {
			return "";
		}
	}
}
