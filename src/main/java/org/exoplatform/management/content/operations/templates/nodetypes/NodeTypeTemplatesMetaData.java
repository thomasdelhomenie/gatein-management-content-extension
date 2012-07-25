package org.exoplatform.management.content.operations.templates.nodetypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NodeTypeTemplatesMetaData {

	private String label;
	private boolean documentTemplate;
	private Map<String, List<Template>> templates;
	
	public NodeTypeTemplatesMetaData() {
		this.templates = new HashMap<String, List<Template>>();
	}

	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public boolean isDocumentTemplate() {
		return documentTemplate;
	}
	
	public void setDocumentTemplate(boolean documentTemplate) {
		this.documentTemplate = documentTemplate;
	}

	public Map<String, List<Template>> getTemplates() {
		return templates;
	}

	public void setTemplates(Map<String, List<Template>> templates) {
		this.templates = templates;
	}
	
	public void addTemplate(String type, Template template) {
		List<Template> typeTemplates = templates.get(type);
		if(typeTemplates == null) {
			typeTemplates = new ArrayList<Template>();
		}
		
		typeTemplates.add(template);
		templates.put(type, typeTemplates);
	}
	
}
