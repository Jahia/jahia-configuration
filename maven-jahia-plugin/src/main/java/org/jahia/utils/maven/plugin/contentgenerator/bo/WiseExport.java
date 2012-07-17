package org.jahia.utils.maven.plugin.contentgenerator.bo;

public class WiseExport extends ExportBO {
	protected Integer nbPolls;
	
	protected Integer nbTasks;
	
	protected Integer nbNotes;
	
	protected Integer nbDocspaces;
	
	protected Integer nbFoldersPerLevel;

	protected Integer foldersDepth;

	protected Integer nbFilesPerFolder;
		
	public void setWiseInstanceKey(String wiseInstanceKey) {
		this.setSiteKey(wiseInstanceKey);
	}
	
	public String getWiseInstanceKey() {
		return this.getSiteKey();
	}
	
	public Integer getNbPolls() {
		return nbPolls;
	}

	public void setNbPolls(Integer nbPolls) {
		this.nbPolls = nbPolls;
	}

	public Integer getNbTasks() {
		return nbTasks;
	}

	public void setNbTasks(Integer nbTasks) {
		this.nbTasks = nbTasks;
	}

	public Integer getNbNotes() {
		return nbNotes;
	}

	public void setNbNotes(Integer nbNotes) {
		this.nbNotes = nbNotes;
	}

	public Integer getNbDocspaces() {
		return nbDocspaces;
	}

	public void setNbDocspaces(Integer nbDocspaces) {
		this.nbDocspaces = nbDocspaces;
	}

	public Integer getNbFoldersPerLevel() {
		return nbFoldersPerLevel;
	}

	public void setNbFoldersPerLevel(Integer nbFoldersPerLevel) {
		this.nbFoldersPerLevel = nbFoldersPerLevel;
	}

	public Integer getFoldersDepth() {
		return foldersDepth;
	}

	public void setFoldersDepth(Integer foldersDepth) {
		this.foldersDepth = foldersDepth;
	}

	public Integer getNbFilesPerFolder() {
		return nbFilesPerFolder;
	}

	public void setNbFilesPerFolder(Integer nbFilesPerFolder) {
		this.nbFilesPerFolder = nbFilesPerFolder;
	}
}
