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

package org.exoplatform.management.content.operations.drive;

import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.drives.impl.ManageDrivePlugin;
import org.exoplatform.services.cms.drives.impl.ManageDriveServiceImpl;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class DriveImportResource implements OperationHandler {
  private ManageDriveService driveService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (driveService == null) {
      driveService = operationContext.getRuntimeContext().getRuntimeComponent(ManageDriveService.class);
    }
    OperationAttachment attachment = operationContext.getAttachment(false);
    InputStream attachmentInputStream = attachment.getStream();
    if (attachmentInputStream == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for taxonomy import.");
    }

    try {

      ZipInputStream zin = new ZipInputStream(attachmentInputStream);
      ZipEntry ze = null;
      while ((ze = zin.getNextEntry()) != null) {
        if (ze.getName().endsWith("drives-configuration.xml")) {
          IBindingFactory bfact = BindingDirectory.getFactory(Configuration.class);
          IUnmarshallingContext uctx = bfact.createUnmarshallingContext();
          Configuration configuration = (Configuration) uctx.unmarshalDocument(zin, "UTF-8");
          ExternalComponentPlugins externalComponentPlugins = configuration.getExternalComponentPlugins(ManageDriveService.class
              .getName());
          List<ComponentPlugin> componentPlugins = externalComponentPlugins.getComponentPlugins();
          for (ComponentPlugin componentPlugin : componentPlugins) {
            Class<?> pluginClass = Class.forName(componentPlugin.getType());
            ManageDrivePlugin cplugin = (ManageDrivePlugin) PortalContainer.getInstance().createComponent(pluginClass,
                componentPlugin.getInitParams());
            cplugin.setName(componentPlugin.getName());
            cplugin.setDescription(componentPlugin.getDescription());
            // TODO add setManageDrivePlugin in Interface ManageDriveService
            ((ManageDriveServiceImpl)driveService).setManageDrivePlugin(cplugin);
          }
        }
      }
      driveService.init();
      zin.close();
      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Exception exception) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing ECMS drives.", exception);
    }
  }

}
