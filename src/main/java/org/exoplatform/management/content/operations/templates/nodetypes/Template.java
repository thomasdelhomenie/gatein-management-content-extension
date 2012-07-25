package org.exoplatform.management.content.operations.templates.nodetypes;

public class Template {
	private String templateFile;
	private String roles;
	
	public Template(String templateFile, String roles) {
		super();
		this.templateFile = templateFile;
		this.roles = roles;
	}
	public String getTemplateFile() {
		return templateFile;
	}
	public void setTemplateFile(String templateFile) {
		this.templateFile = templateFile;
	}
	public String getRoles() {
		return roles;
	}
	public void setRoles(String roles) {
		this.roles = roles;
	}
}
