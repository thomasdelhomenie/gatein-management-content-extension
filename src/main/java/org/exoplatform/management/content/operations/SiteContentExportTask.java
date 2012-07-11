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
import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.jcr.RepositoryService;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SiteContentExportTask implements ExportTask
{
   private final RepositoryService repositoryService;
   private final String workspace;
   private final String absolutePath;

   public SiteContentExportTask(RepositoryService repositoryService, String workspace, String absolutePath)
   {
      this.repositoryService = repositoryService;
      this.workspace = workspace;
      this.absolutePath = absolutePath;
   }
   
   @Override
   public String getEntry() {
     return "sysview.xml";
   }

   @Override
   public void export(OutputStream outputStream) throws IOException
   {
     try {
      Session session = repositoryService.getCurrentRepository().getSystemSession(workspace);
    } catch (RepositoryException exception) {
      // TODO Auto-generated catch block
      exception.printStackTrace();
    }
//     session.exportSystemView(absolutePath, contentHandler, skipBinary, noRecurse);

   }

}
