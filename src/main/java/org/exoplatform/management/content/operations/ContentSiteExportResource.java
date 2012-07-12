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

package org.exoplatform.management.content.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.portal.PortalFolderSchemaHandler;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas Delhoménie</a>
 * @version $Revision$
 */
public class ContentSiteExportResource implements OperationHandler {

	@Override
	public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
		try {
			String operationName = operationContext.getOperationName();
			PathAddress address = operationContext.getAddress();
			OperationAttributes attributes = operationContext.getAttributes();

			String siteName = address.resolvePathTemplate("site-name");
			if (siteName == null) {
				throw new OperationException(operationName, "No site name specified.");
			}

			WCMConfigurationService wcmConfigurationService = operationContext.getRuntimeContext().getRuntimeComponent(WCMConfigurationService.class);
			Collection<NodeLocation> sitesLocations = wcmConfigurationService.getAllLivePortalsLocation();
			if (sitesLocations == null || sitesLocations.size() != 1) {
				throw new OperationException(operationName,
						"Unable to read site locations, expected one slocation config, site location config found = " + sitesLocations);
			}
			NodeLocation sitesLocation = sitesLocations.iterator().next();
			String siteRootNodePath = sitesLocation.getPath();
			if (!siteRootNodePath.endsWith("/")) {
				siteRootNodePath += "/";
			}
			siteRootNodePath += siteName;

			List<ExportTask> exportTasks = new ArrayList<ExportTask>();

			RepositoryService repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);

			boolean exportWholeSite = attributes.getValues("filter").contains("scope:all");
			if (!exportWholeSite) {
				exportTasks.addAll(exportSiteWithoutSkeleton(sitesLocation, siteRootNodePath, repositoryService));
			} else {
				exportTasks.addAll(exportSite(sitesLocation, siteRootNodePath, repositoryService));
			}

			resultHandler.completed(new ExportResourceModel(exportTasks));
		} catch (Exception e) {
			throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to retrieve the list of the contents sites : " + e.getMessage());
		}
	}

	/**
	 * @param sitesLocation
	 * @param siteRootNodePath
	 * @param exportTasks
	 * @param repositoryService
	 */
	private List<ExportTask> exportSite(NodeLocation sitesLocation, String siteRootNodePath, RepositoryService repositoryService) {
		List<ExportTask> exportTasks = new ArrayList<ExportTask>();
		exportTasks.add(new SiteContentExportTask(repositoryService, sitesLocation.getWorkspace(), siteRootNodePath));
		return exportTasks;
	}

	/**
	 * @param sitesLocation
	 * @param path
	 * @param exportTasks
	 * @param repositoryService
	 * @throws Exception
	 * @throws RepositoryException
	 */
	private List<ExportTask> exportSiteWithoutSkeleton(NodeLocation sitesLocation, String path, RepositoryService repositoryService)
			throws Exception, RepositoryException {

		List<ExportTask> exportTasks = new ArrayList<ExportTask>();

		NodeLocation nodeLocation = new NodeLocation("repository", sitesLocation.getWorkspace(), path, null, true);
		Node portalNode = NodeLocation.getNodeByLocation(nodeLocation);

		PortalFolderSchemaHandler portalFolderSchemaHandler = new PortalFolderSchemaHandler();

		// CSS Folder
		exportTasks.addAll(exportSubNodes(repositoryService, sitesLocation.getWorkspace(), portalFolderSchemaHandler.getCSSFolder(portalNode), null));

		// JS Folder
		exportTasks.addAll(exportSubNodes(repositoryService, sitesLocation.getWorkspace(), portalFolderSchemaHandler.getJSFolder(portalNode), null));

		// Document Folder
		exportTasks.addAll(exportSubNodes(repositoryService, sitesLocation.getWorkspace(), portalFolderSchemaHandler.getDocumentStorage(portalNode), null));

		// Images Folder
		exportTasks.addAll(exportSubNodes(repositoryService, sitesLocation.getWorkspace(), portalFolderSchemaHandler.getImagesFolder(portalNode), null));

		// Audio Folder
		exportTasks.addAll(exportSubNodes(repositoryService, sitesLocation.getWorkspace(), portalFolderSchemaHandler.getAudioFolder(portalNode), null));

		// Video Folder
		exportTasks.addAll(exportSubNodes(repositoryService, sitesLocation.getWorkspace(), portalFolderSchemaHandler.getVideoFolder(portalNode), null));

		// Multimedia Folder
		exportTasks.addAll(exportSubNodes(repositoryService, sitesLocation.getWorkspace(), portalFolderSchemaHandler.getMultimediaFolder(portalNode),
				Arrays.asList("images", "audio", "videos")));

		// Link Folder
		exportTasks.addAll(exportSubNodes(repositoryService, sitesLocation.getWorkspace(), portalFolderSchemaHandler.getLinkFolder(portalNode), null));

		// WebContent Folder
		exportTasks.addAll(exportSubNodes(repositoryService, sitesLocation.getWorkspace(), portalFolderSchemaHandler.getWebContentStorage(portalNode),
				Arrays.asList("site-artifacts")));

		// Site Artifacts Folder
		Node webContentNode = portalFolderSchemaHandler.getWebContentStorage(portalNode);
		exportTasks.addAll(exportSubNodes(repositoryService, sitesLocation.getWorkspace(), webContentNode.getNode("site artifacts"), null));

		// Categories Folder
		Node categoriesNode = portalNode.getNode("categories");
		exportTasks.addAll(exportSubNodes(repositoryService, sitesLocation.getWorkspace(), categoriesNode, null));
		
		return exportTasks;
	}

	
	/**
	 * Export all sub-nodes of the given node
	 * @param repositoryService
	 * @param workspace
	 * @param parentNode
	 * @param excludedNodes
	 * @return
	 * @throws RepositoryException
	 */
	protected List<ExportTask> exportSubNodes(RepositoryService repositoryService, String workspace, Node parentNode, List<String> excludedNodes)
			throws RepositoryException {

		List<ExportTask> subNodesExportTask = new ArrayList<ExportTask>();

		NodeIterator childrenNodes = parentNode.getNodes();
		while (childrenNodes.hasNext()) {
			Node childNode = (Node) childrenNodes.next();
			if (excludedNodes == null || !excludedNodes.contains(childNode.getName())) {
				subNodesExportTask.add(new SiteContentExportTask(repositoryService, workspace, childNode.getPath()));
			}
		}

		return subNodesExportTask;
	}
}
