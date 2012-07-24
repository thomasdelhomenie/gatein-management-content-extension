package org.exoplatform.management.content.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;
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
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class ContentSiteExportResource implements OperationHandler {

	private SiteMetaData metaData = null;

	@Override
	public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
		try {
			metaData = new SiteMetaData();
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
				throw new OperationException(operationName, "Unable to read site locations, expected one slocation config, site location config found = "
						+ sitesLocations);
			}
			NodeLocation sitesLocation = sitesLocations.iterator().next();
			String sitePath = sitesLocation.getPath();
			if (!sitePath.endsWith("/")) {
				sitePath += "/";
			}
			sitePath += siteName;

			metaData.getOptions().put(SiteMetaData.SITE_PATH, sitePath);
			metaData.getOptions().put(SiteMetaData.SITE_WORKSPACE, sitesLocation.getWorkspace());
			metaData.getOptions().put(SiteMetaData.SITE_NAME, siteName);

			List<ExportTask> exportTasks = new ArrayList<ExportTask>();

			RepositoryService repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);

			List<String> filters = attributes.getValues("filter");
			
			// scope filter
			List<String> scopes = getScopes(filters);
			boolean exportAll = scopes.isEmpty() || scopes.contains("all");
			boolean exportSiteContents = exportAll || scopes.contains("site-contents");
			boolean exportSEO = exportAll || scopes.contains("seo");
			
			boolean exportSiteWithSkeleton = !filters.contains("no-skeleton:true");
			
			// Site contents
			if(exportSiteContents) {
				if (exportSiteWithSkeleton) {
					exportTasks.addAll(exportSite(sitesLocation, sitePath, repositoryService));
				} else {
					exportTasks.addAll(exportSiteWithoutSkeleton(sitesLocation, sitePath, repositoryService));
				}
			}
			
			// SEO metadata on pages
			if(exportSEO) {
				exportTasks.add(getSEOExportTask(operationContext, siteName));
			}
			
			// Metadata
			exportTasks.add(getMetaDataExportTask());

			resultHandler.completed(new ExportResourceModel(exportTasks));
		} catch (Exception e) {
			throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to retrieve the list of the contents sites : " + e.getMessage());
		}
	}

	/**
	 * Extract scopes from the command filters
	 * @param filters filters
	 * @return List of scopes
	 */
	private List<String> getScopes(List<String> filters) {
		List<String> scopes = new ArrayList<String>();
		for(String filter : filters) {
			if(filter.startsWith("scope:")) {
				String[] scopesArray = filter.substring("scope:".length()).split(",");
				scopes = Arrays.asList(scopesArray);
				break;
			}
		}
		return scopes;
	}

	/**
	 * @param sitesLocation
	 * @param siteRootNodePath
	 * @param exportTasks
	 * @param repositoryService
	 */
	private List<ExportTask> exportSite(NodeLocation sitesLocation, String siteRootNodePath, RepositoryService repositoryService) {
		List<ExportTask> exportTasks = new ArrayList<ExportTask>();
		
		SiteContentExportTask siteContentExportTask = new SiteContentExportTask(repositoryService, sitesLocation.getWorkspace(), siteRootNodePath);
		exportTasks.add(siteContentExportTask);

		metaData.getExportedFiles().put(siteContentExportTask.getEntry(), sitesLocation.getPath());
		
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
	private List<ExportTask> exportSiteWithoutSkeleton(NodeLocation sitesLocation, String path, RepositoryService repositoryService) throws Exception,
			RepositoryException {

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
				Arrays.asList("site artifacts")));

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
	 * 
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
				SiteContentExportTask siteContentExportTask = new SiteContentExportTask(repositoryService, workspace, childNode.getPath());
				subNodesExportTask.add(siteContentExportTask);
				metaData.getExportedFiles().put(siteContentExportTask.getEntry(), parentNode.getPath());
			}
		}

		return subNodesExportTask;
	}

	@SuppressWarnings("unused")
	private SiteSEOExportTask getSEOExportTask(OperationContext operationContext, String siteName) throws Exception {
		DataStorage dataStorage = operationContext.getRuntimeContext().getRuntimeComponent(DataStorage.class);
		LazyPageList<Page> pagLazyList = dataStorage.find(new Query<Page>(SiteType.PORTAL.getName(), siteName, Page.class));
		SEOService seoService = operationContext.getRuntimeContext().getRuntimeComponent(SEOService.class);
		List<Page> pageList = pagLazyList.getAll();
		List<PageMetadataModel> pageMetadataModels = new ArrayList<PageMetadataModel>();
		for (Page page : pageList) {
			PageMetadataModel pageMetadataModel = null;// seoService.getPageMetadata(page.getPageId());
			if (pageMetadataModel != null && pageMetadataModel.getKeywords() != null && !pageMetadataModel.getKeywords().isEmpty()) {
				pageMetadataModels.add(pageMetadataModel);
			}
		}

		return new SiteSEOExportTask(pageMetadataModels);
	}

	private SiteMetaDataExportTask getMetaDataExportTask() {
		return new SiteMetaDataExportTask(metaData);
	}
}
