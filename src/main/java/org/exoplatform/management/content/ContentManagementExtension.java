package org.exoplatform.management.content;

import org.exoplatform.management.content.operations.ContentSiteExportResource;
import org.exoplatform.management.content.operations.ContentSiteReadResource;
import org.exoplatform.management.content.operations.LiveSitesReadResource;
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
    content.registerOperationHandler(OperationNames.READ_RESOURCE, new LiveSitesReadResource(),
        description("Lists available contents sites"));

    ManagedResource.Registration sites = content.registerSubResource("{site-name: .*}", description("Management resource responsible for handling management operations on a specific site."));
    sites.registerOperationHandler(OperationNames.READ_RESOURCE, new ContentSiteReadResource(), description("Lists available resources for a given site (ie pages, navigation, site layout)"));

    sites.registerOperationHandler(OperationNames.EXPORT_RESOURCE, new ContentSiteExportResource(),
        description("Lists available contents sites"));
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
