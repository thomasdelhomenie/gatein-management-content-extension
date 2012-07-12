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

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SiteContentExportTask implements ExportTask {
  private final RepositoryService repositoryService;
  private final String workspace;
  private final String absolutePath;

  public SiteContentExportTask(RepositoryService repositoryService, String workspace, String absolutePath) {
    this.repositoryService = repositoryService;
    this.workspace = workspace;
    this.absolutePath = absolutePath;
  }

  @Override
  public String getEntry() {
    return "contents-sysview" + absolutePath;
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    Session session = null;
    try {
      session = repositoryService.getCurrentRepository().getSystemSession(workspace);
      session.exportSystemView(absolutePath, outputStream, false, false);
    } catch (RepositoryException exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export content from : " + absolutePath);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

}
