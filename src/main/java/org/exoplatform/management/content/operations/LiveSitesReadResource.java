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

import java.util.HashSet;
import java.util.Set;

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class LiveSitesReadResource implements OperationHandler {
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    try {
      UserPortalConfigService portalConfigService = operationContext.getRuntimeContext().getRuntimeComponent(
          UserPortalConfigService.class);
      Set<String> sites = new HashSet<String>(portalConfigService.getAllPortalNames());

      WCMConfigurationService wcmConfigurationService = operationContext.getRuntimeContext().getRuntimeComponent(
          WCMConfigurationService.class);
      sites.add(wcmConfigurationService.getSharedPortalName());

      resultHandler.completed(new ReadResourceModel("Available sites.", sites));
    } catch (Exception e) {
      throw new OperationException(OperationNames.READ_RESOURCE, "Unable to retrieve the list of the sites : " + e.getMessage());
    }
  }
}
