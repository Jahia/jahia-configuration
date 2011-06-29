package org.jahia.utils.maven.plugin.contentgenerator.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jahia.utils.maven.plugin.contentgenerator.ContentGeneratorService;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;

/**
 * Generates files that can be used as attachments
 * 
 * @goal generate-files
 * @requiresProject false
 */
public class GenerateFilesMojo extends ContentGeneratorMojo {
	public void execute() throws MojoExecutionException, MojoFailureException {
		ContentGeneratorService contentGeneratorService = ContentGeneratorService.getInstance();
		ExportBO export = super.initExport();
		contentGeneratorService.generateFiles(export);
	}
}
