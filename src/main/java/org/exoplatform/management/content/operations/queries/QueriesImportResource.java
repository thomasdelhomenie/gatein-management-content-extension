package org.exoplatform.management.content.operations.queries;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.query.Query;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.cms.queries.impl.QueryData;
import org.exoplatform.services.cms.queries.impl.QueryPlugin;
import org.exoplatform.services.cms.queries.impl.QueryServiceImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
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
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas Delhoménie</a>
 */
public class QueriesImportResource implements OperationHandler {
	private QueryService queryService;

	private static final Log log = ExoLogger.getLogger(QueriesImportResource.class);

	@Override
	public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {

		if (queryService == null) {
			queryService = operationContext.getRuntimeContext().getRuntimeComponent(QueryService.class);
		}
		OperationAttachment attachment = operationContext.getAttachment(false);
		InputStream attachmentInputStream = attachment.getStream();
		if (attachmentInputStream == null) {
			throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for queries import.");
		}

		try {

			ZipInputStream zin = new ZipInputStream(attachmentInputStream);
			ZipEntry ze = null;

			IBindingFactory bfact = BindingDirectory.getFactory(Configuration.class);
			IUnmarshallingContext uctx = bfact.createUnmarshallingContext();

			while ((ze = zin.getNextEntry()) != null) {
				String zipEntryName = ze.getName();
				if (zipEntryName.startsWith("queries/")) {

					Configuration configuration = (Configuration) uctx.unmarshalDocument(zin, "UTF-8");
					ExternalComponentPlugins externalComponentPlugins = configuration.getExternalComponentPlugins(QueryService.class.getName());
					List<ComponentPlugin> componentPlugins = externalComponentPlugins.getComponentPlugins();

					// Users' queries
					if (zipEntryName.startsWith("queries/users/") && zipEntryName.endsWith("-queries-configuration.xml")) {
						// extract username from filename
						String username = zipEntryName.substring(zipEntryName.lastIndexOf("/") + 1, zipEntryName.indexOf("-queries-configuration.xml"));

						List<Query> queries = queryService.getQueries(username, WCMCoreUtils.getSystemSessionProvider());
						
						// Can't create user's queries via configuration (only shared), so they are directly created
						// via the queryService.addQuery method
						for (ComponentPlugin componentPlugin : componentPlugins) {
							@SuppressWarnings("rawtypes")
							Iterator objectParamIterator = componentPlugin.getInitParams().getObjectParamIterator();
							while (objectParamIterator.hasNext()) {
								ObjectParameter objectParam = (ObjectParameter) objectParamIterator.next();
								Object object = objectParam.getObject();
								if (object instanceof QueryData) {
									QueryData queryData = (QueryData) object;
									boolean alreadyExists = false;
									for(Query query : queries) {
										if(queryData.getName().equals(query.getStoredQueryPath().substring(query.getStoredQueryPath().lastIndexOf("/") + 1))) {
											// query already exists -> ignored
											log.warn("Query " + queryData.getName() + " already exists for user " + username + " -> ignored");
											alreadyExists = true;
											break;
										}
									}
									if(!alreadyExists) {
										queryService.addQuery(queryData.getName(), queryData.getStatement(), queryData.getLanguage(), username);
									}
								}
							}
						}
					} else if (zipEntryName.endsWith("shared-queries-configuration.xml")) {

						for (ComponentPlugin componentPlugin : componentPlugins) {
							Class<?> pluginClass = Class.forName(componentPlugin.getType());
							@SuppressWarnings("rawtypes")
							Iterator objectParamIterator = componentPlugin.getInitParams().getObjectParamIterator();
							while (objectParamIterator.hasNext()) {
								ObjectParameter objectParam = (ObjectParameter) objectParamIterator.next();
								Object object = objectParam.getObject();
								if (object instanceof QueryData) {
									QueryData queryData = (QueryData) object;
									// if the shared query already exists, remove it from the init-params of the plugin
									if (queryService.getAllConfiguredQueries().contains(queryData.getName())) {
										log.warn("Shared query " + queryData.getName() + " already exists -> ignored");
										componentPlugin.getInitParams().removeParameter(queryData.getName());
									}
								}
							}
							QueryPlugin cplugin = (QueryPlugin) PortalContainer.getInstance().createComponent(pluginClass, componentPlugin.getInitParams());
							cplugin.setName(componentPlugin.getName());
							cplugin.setDescription(componentPlugin.getDescription());
							// TODO add setQueryPlugin in Interface QueryService
							((QueryServiceImpl) queryService).setQueryPlugin(cplugin);
						}
					}
				}
			}
			// init service, so it will create the shared queries
			queryService.init();
			zin.close();
			resultHandler.completed(NoResultModel.INSTANCE);
		} catch (Exception exception) {
			throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing ECMS queries.", exception);
		}
	}
}
