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

package org.exoplatform.management.content.operations.site.taxonomy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.management.content.operations.templates.NodeTemplate;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class TaxonomyImportResource implements OperationHandler {

  private static Map<String, TaxonomyMetaData> metadataMap = new HashMap<String, TaxonomyMetaData>();
  private static Map<String, File> exportMap = new HashMap<String, File>();

  final private static Logger log = LoggerFactory.getLogger(TaxonomyImportResource.class);

  private String operationName = null;
  private TaxonomyService taxonomyService;
  private RepositoryService repositoryService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {

    if (taxonomyService == null) {
      taxonomyService = operationContext.getRuntimeContext().getRuntimeComponent(TaxonomyService.class);
    }
    if (repositoryService == null) {
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    }
    operationName = operationContext.getOperationName();

    OperationAttachment attachment = operationContext.getAttachment(false);
    InputStream attachmentInputStream = attachment.getStream();
    if (attachmentInputStream == null) {
      throw new OperationException(this.operationName, "No data stream available for taxonomy import.");
    }
    // "uuidBehavior" attribute
    int uuidBehaviorValue = extractUuidBehavior(operationContext.getAttributes().getValue("uuidBehavior"));

    try {
      ZipInputStream zin = new ZipInputStream(attachmentInputStream);
      ZipEntry ze = null;
      while ((ze = zin.getNextEntry()) != null) {
        if (ze.getName().endsWith("tree.xml")) {
          File tempFile = File.createTempFile("jcr", "sysview");
          FileOutputStream fout = new FileOutputStream(tempFile);
          for (int c = zin.read(); c != -1; c = zin.read()) {
            fout.write(c);
          }
          zin.closeEntry();
          fout.close();
          String taxonomyName = extractTxonomyName(ze.getName());
          exportMap.put(taxonomyName, tempFile);
        } else if (ze.getName().endsWith("metadata.xml")) {
          ByteArrayOutputStream fout = new ByteArrayOutputStream();
          for (int c = zin.read(); c != -1; c = zin.read()) {
            fout.write(c);
          }
          zin.closeEntry();
          String taxonomyName = extractTxonomyName(ze.getName());

          XStream xStream = new XStream();
          xStream.alias("metadata", TaxonomyMetaData.class);
          xStream.alias("taxonomy", NodeTemplate.class);
          TaxonomyMetaData taxonomyMetaData = (TaxonomyMetaData) xStream.fromXML(fout.toString("UTF-8"));
          metadataMap.put(taxonomyName, taxonomyMetaData);
        }
      }
      zin.close();

      for (Entry<String, TaxonomyMetaData> entry : metadataMap.entrySet()) {
        String taxonomyName = entry.getKey();
        if (taxonomyService.hasTaxonomyTree(taxonomyName)) {
          log.warn("Taxonomy tree '" + taxonomyName + "' already exists, don't proceed to import operation.");
          continue;
        }
        TaxonomyMetaData metaData = entry.getValue();

        SessionProvider sessionProvider = SessionProvider.createSystemProvider();
        Session session = sessionProvider.getSession(metaData.getTaxoTreeWorkspace(), repositoryService.getCurrentRepository());
        int length = metaData.getTaxoTreeHomePath().lastIndexOf("/" + taxonomyName) + 1;
        String absolutePath = metaData.getTaxoTreeHomePath().substring(0, length);
        session.importXML(absolutePath, new FileInputStream(exportMap.get(taxonomyName)), uuidBehaviorValue);
        session.save();

        Node taxonomyNode = (Node) session.getItem(metaData.getTaxoTreeHomePath());
        taxonomyService.addTaxonomyTree(taxonomyNode);
      }
      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Throwable exception) {
      exception.printStackTrace();
    }
  }

  /**
   * Convert UUID behavior string into int
   * 
   * @param uuidBehavior
   * @return
   */
  private int extractUuidBehavior(String uuidBehavior) {
    int uuidBehaviorValue = -1;
    if (!StringUtils.isEmpty(uuidBehavior)) {
      uuidBehaviorValue = Integer.valueOf(uuidBehavior);
      if (uuidBehaviorValue > 3 || uuidBehaviorValue < 0) {
        uuidBehaviorValue = -1;
        log.warn("UUID Behavior must be '<=3' and '>=0', CREATE NEW UUID Behavior will be used instead.");
      }
    }
    if (uuidBehaviorValue == -1) {
      uuidBehaviorValue = ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;
    }
    return uuidBehaviorValue;
  }

  private static String extractTxonomyName(String name) {
    return name.split("/")[1];
  }

}
