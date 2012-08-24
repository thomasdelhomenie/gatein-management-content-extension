package org.exoplatform.management.content;

import org.exoplatform.management.content.operations.ContentReadResource;
import org.exoplatform.management.content.operations.queries.QueriesExportResource;
import org.exoplatform.management.content.operations.queries.QueriesReadResource;
import org.exoplatform.management.content.operations.site.LiveSitesReadResource;
import org.exoplatform.management.content.operations.site.SiteReadResource;
import org.exoplatform.management.content.operations.site.seo.SiteSEOExportResource;
import org.exoplatform.management.content.operations.site.seo.SiteSEOReadResource;
import org.exoplatform.management.content.operations.templates.TemplatesReadResource;
import org.exoplatform.management.content.operations.templates.applications.ApplicationTemplatesExportResource;
import org.exoplatform.management.content.operations.templates.applications.ApplicationTemplatesReadResource;
import org.exoplatform.management.content.operations.templates.applications.ApplicationsTemplatesReadResource;
import org.exoplatform.management.content.operations.templates.metadata.MetadataTemplatesExportResource;
import org.exoplatform.management.content.operations.templates.metadata.MetadataTemplatesReadResource;
import org.exoplatform.management.content.operations.templates.nodetypes.NodeTypesTemplatesExportResource;
import org.exoplatform.management.content.operations.templates.nodetypes.NodeTypesTemplatesReadResource;
import org.exoplatform.management.content.operations.site.contents.SiteContentsExportResource;
import org.exoplatform.management.content.operations.site.contents.SiteContentsImportResource;
import org.exoplatform.management.content.operations.site.contents.SiteContentsReadResource;
import org.gatein.management.api.ComponentRegistration;
import org.gatein.management.api.ManagedDescription;
import org.gatein.management.api.ManagedResource;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.spi.ExtensionContext;
import org.gatein.management.spi.ManagementExtension;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class ContentManagementExtension implements ManagementExtension {
  @Override
  public void initialize(ExtensionContext context) {
    ComponentRegistration registration = context.registerManagedComponent("content");

    ManagedResource.Registration content = registration
        .registerManagedResource(description("Content Managed Resource, responsible for handling management operations on contents."));
    content.registerOperationHandler(OperationNames.READ_RESOURCE, new ContentReadResource(),
        description("Lists available contents data"));
    content.registerOperationHandler(OperationNames.IMPORT_RESOURCE, new SiteContentsImportResource(),
            description("Import contents data"));

    // /content/sites
    ManagedResource.Registration sites = content
        .registerSubResource("sites", description("Sites Managed Resource, responsible for handling management operations on sites contents."));
    sites.registerOperationHandler(OperationNames.READ_RESOURCE, new LiveSitesReadResource(),
        description("Lists available sites"));

    // /content/sites/<site_name>
    ManagedResource.Registration site = sites.registerSubResource("{site-name: .*}", description("Management resource responsible for handling management operations on a specific site."));
    site.registerOperationHandler(OperationNames.READ_RESOURCE, new SiteReadResource(), description("Read site"));
    
    // /content/sites/<site_name>/contents
    ManagedResource.Registration siteContents = site.registerSubResource("contents", description("Management resource responsible for handling management operations on contents of a specific site."));
    siteContents.registerOperationHandler(OperationNames.READ_RESOURCE, new SiteContentsReadResource(), description("Read site contents"));
    siteContents.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new SiteContentsExportResource(),
        description("Export site contents"));    
    
    // /content/sites/<site_name>/seo
    ManagedResource.Registration seo = site.registerSubResource("seo", description("Management resource responsible for handling management operations on SEO of a specific site."));
    seo.registerOperationHandler(OperationNames.READ_RESOURCE, new SiteSEOReadResource(), description("Read site SEO data"));
    seo.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new SiteSEOExportResource(),
        description("Export site SEO data"));    

    // /content/templates
    ManagedResource.Registration templates = content
        .registerSubResource("templates", description("Sites Managed Resource, responsible for handling management operations on templates."));
    templates.registerOperationHandler(OperationNames.READ_RESOURCE, new TemplatesReadResource(),
        description("Lists available template types"));

    // /content/templates/applications
    ManagedResource.Registration applicationsTemplates = templates
        .registerSubResource("applications", description("Sites Managed Resource, responsible for handling management operations on applications templates."));
    applicationsTemplates.registerOperationHandler(OperationNames.READ_RESOURCE, new ApplicationsTemplatesReadResource(),
        description("Lists available applications containing templates"));

    // /content/templates/applications/<application_name>
    ManagedResource.Registration applicationTemplates = applicationsTemplates
        .registerSubResource("{application-name: .*}", description("Sites Managed Resource, responsible for handling management operations on templates of an application."));
    applicationTemplates.registerOperationHandler(OperationNames.READ_RESOURCE, new ApplicationTemplatesReadResource(),
        description("Lists available templates of an application"));
    applicationTemplates.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ApplicationTemplatesExportResource(),
            description("Exports available templates of an application"));  
    
    // /content/templates/nodetypes
    ManagedResource.Registration nodetypesTemplates = templates
        .registerSubResource("nodetypes", description("Sites Managed Resource, responsible for handling management operations on node types templates."));
    nodetypesTemplates.registerOperationHandler(OperationNames.READ_RESOURCE, new NodeTypesTemplatesReadResource(),
        description("Lists available node types templates"));
    nodetypesTemplates.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new NodeTypesTemplatesExportResource(),
            description("Exports available node types templates"));
    
    // /content/templates/metadata
    ManagedResource.Registration metadataTemplates = templates
        .registerSubResource("metadata", description("Sites Managed Resource, responsible for handling management operations on metadata templates."));
    metadataTemplates.registerOperationHandler(OperationNames.READ_RESOURCE, new MetadataTemplatesReadResource(),
        description("Lists available metadata templates"));
    metadataTemplates.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new MetadataTemplatesExportResource(),
            description("Exports available metadata templates"));    
    
    // /content/queries
    ManagedResource.Registration queries = content
        .registerSubResource("queries", description("Queries Managed Resource, responsible for handling management operations on queries."));
    queries.registerOperationHandler(OperationNames.READ_RESOURCE, new QueriesReadResource(),
        description("Lists available queries"));
    queries.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new QueriesExportResource(),
            description("Exports available queries"));
  }

  @Override
  public void destroy() {}

  private static ManagedDescription description(final String description) {
    return new ManagedDescription() {
      @Override
      public String getDescription() {
        return description;
      }
    };
  }
}
