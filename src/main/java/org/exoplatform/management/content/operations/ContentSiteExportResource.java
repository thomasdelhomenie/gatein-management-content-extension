package org.exoplatform.management.content.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
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
    String siteName = null;
    try {
      metaData = new SiteMetaData();
      String operationName = operationContext.getOperationName();
      PathAddress address = operationContext.getAddress();

      siteName = address.resolvePathTemplate("site-name");
      List<ExportTask> exportTasks = getSiteContentsExportTasks(operationContext, siteName, operationName);
      exportTasks.add(getSEOExportTask(operationContext, siteName));
      exportTasks.add(getMetaDataExportTask());

      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to retrieve the list of the seo of site " + siteName
          + ": " + e.getMessage(), e);
    }
  }

  private List<ExportTask> getSiteContentsExportTasks(OperationContext operationContext, String siteName, String operationName) {
    if (siteName == null) {
      throw new OperationException(operationName, "No site name specified.");
    }
    List<ExportTask> exportTasks = new ArrayList<ExportTask>();

    WCMConfigurationService wcmConfigurationService = operationContext.getRuntimeContext().getRuntimeComponent(
        WCMConfigurationService.class);
    Collection<NodeLocation> sitesLocations = wcmConfigurationService.getAllLivePortalsLocation();
    if (sitesLocations == null || sitesLocations.size() != 1) {
      throw new OperationException(operationName,
          "Unable to read site locations, expected one slocation config, site location config found = " + sitesLocations);
    }
    NodeLocation sitesLocation = sitesLocations.iterator().next();
    String path = sitesLocation.getPath();
    if (!path.endsWith("/")) {
      path += "/";
    }
    path += siteName;

    metaData.getOptions().put(SiteMetaData.SITE_PATH, path);
    metaData.getOptions().put(SiteMetaData.SITE_WORKSPACE, sitesLocation.getWorkspace());
    metaData.getOptions().put(SiteMetaData.SITE_NAME, siteName);

    RepositoryService repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    SiteContentExportTask exportTask = new SiteContentExportTask(repositoryService, sitesLocation.getWorkspace(), path);
    metaData.getJcrExportedFiles().put(exportTask.getEntry(), path);

    exportTasks.add(exportTask);
    return exportTasks;
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
