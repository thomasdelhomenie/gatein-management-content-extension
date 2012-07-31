package org.exoplatform.management.content.operations.site.contents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.portal.PortalFolderSchemaHandler;
import org.exoplatform.wcm.webui.Utils;
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
public class SiteContentsExportResource implements OperationHandler {
  private static final Log log = ExoLogger.getLogger(SiteContentsExportResource.class);
  
  private static final String FOLDER_PATH = "folderPath";
  public static String WORKSPACE = "workspace";
  public static String IDENTIFIER = "nodeIdentifier";
  public static final String FILTER_SEPARATOR = ":";

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

      boolean exportSiteWithSkeleton = !filters.contains("no-skeleton:true");

      // Validate Site Structure
      DataStorage dataStorage = operationContext.getRuntimeContext().getRuntimeComponent(DataStorage.class);
      validateSiteStructure(siteName, dataStorage);
      
      // Site contents
      if (exportSiteWithSkeleton) {
        exportTasks.addAll(exportSite(sitesLocation, sitePath, repositoryService));
      } else {
        exportTasks.addAll(exportSiteWithoutSkeleton(sitesLocation, sitePath, repositoryService));
      }

      // Metadata
			exportTasks.add(getMetaDataExportTask());

      resultHandler.completed(new ExportResourceModel(exportTasks));
		} catch (Exception e) {
			throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to retrieve the list of the contents sites : " + e.getMessage());
		}
  }

  private void validateSiteStructure(String siteName, DataStorage dataStorage) throws Exception {
    LazyPageList<Page> pagLazyList = dataStorage.find(new Query<Page>(SiteType.PORTAL.getName(), siteName, Page.class));
    List<Page> pageList = pagLazyList.getAll();
    Set<String> contentSet = new HashSet<String>();
    for (Page page : pageList) {
      contentSet.addAll(getSCVPaths(page.getChildren(), dataStorage));
      contentSet.addAll(getCLVPaths(page.getChildren(), dataStorage));
    }
    if(!contentSet.isEmpty()) {
      log.info("There are some contents used in pages that don't belong to site's JCR structure: " + contentSet);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private List<String> getSCVPaths(ArrayList<ModelObject> children, DataStorage dataStorage) throws Exception {
    List<String> scvPaths = new ArrayList<String>();
    if (children != null) {
      for (ModelObject modelObject : children) {
        if (modelObject instanceof Application) {
          Portlet portlet = (Portlet) dataStorage.load(((Application) modelObject).getState(),
              ((Application) modelObject).getType());
          if (portlet.getValue(IDENTIFIER) == null) {
            continue;
          }
          String workspace = portlet.getPreference(WORKSPACE).getValue();
          String nodeIdentifier = portlet.getPreference(IDENTIFIER).getValue();
          if (workspace.equals(metaData.getOptions().get(SiteMetaData.SITE_WORKSPACE))
              && nodeIdentifier.startsWith(metaData.getOptions().get(SiteMetaData.SITE_PATH))) {
            continue;
          }
          String path = Utils.getRealNode(portlet.getPreference("repository").getValue(), workspace, nodeIdentifier, true)
              .getPath();
          scvPaths.add(path);
        } else if (modelObject instanceof Container) {
          scvPaths.addAll(getSCVPaths(((Container) modelObject).getChildren(), dataStorage));
        }
      }
    }
    return scvPaths;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private List<String> getCLVPaths(ArrayList<ModelObject> children, DataStorage dataStorage) throws Exception {
    List<String> scvPaths = new ArrayList<String>();
    if (children != null) {
      for (ModelObject modelObject : children) {
        if (modelObject instanceof Application) {
          Portlet portlet = (Portlet) dataStorage.load(((Application) modelObject).getState(),
              ((Application) modelObject).getType());
          if (portlet.getValue(FOLDER_PATH) == null) {
            continue;
          }
          String[] folderPaths = portlet.getPreference(FOLDER_PATH).getValue().split(";");
          for (String folderPath : folderPaths) {
            String[] paths = folderPath.split(":");
            String workspace = paths[1];
            String path = paths[2];
            if (workspace.equals(metaData.getOptions().get(SiteMetaData.SITE_WORKSPACE))
                && path.startsWith(metaData.getOptions().get(SiteMetaData.SITE_PATH))) {
              continue;
            }
            scvPaths.add(path);
          }
        } else if (modelObject instanceof Container) {
          scvPaths.addAll(getCLVPaths(((Container) modelObject).getChildren(), dataStorage));
        }
      }
    }
    return scvPaths;
  }

  /**
   * @param sitesLocation
   * @param siteRootNodePath
   * @param exportTasks
   * @param repositoryService
   */
	private List<ExportTask> exportSite(NodeLocation sitesLocation, String siteRootNodePath, RepositoryService repositoryService) {
		List<ExportTask> exportTasks = new ArrayList<ExportTask>();
		
		SiteContentsExportTask siteContentExportTask = new SiteContentsExportTask(repositoryService, sitesLocation.getWorkspace(), siteRootNodePath);
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
				SiteContentsExportTask siteContentExportTask = new SiteContentsExportTask(repositoryService, workspace, childNode.getPath());
				subNodesExportTask.add(siteContentExportTask);
				metaData.getExportedFiles().put(siteContentExportTask.getEntry(), parentNode.getPath());
			}
		}

		return subNodesExportTask;
	}

	private SiteMetaDataExportTask getMetaDataExportTask() {
		return new SiteMetaDataExportTask(metaData);
	}
}
