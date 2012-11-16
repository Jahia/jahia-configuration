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
public class GenerateFilesMojo extends AbstractContentGeneratorMojo {
	
	/**
	 * Number of files to generate (text files filled with a random Wikipedia article)
	 * Required for goal "generate-files"
	 * @parameter expression="${jahia.cg.numberOfFilesToGenerate}" default-value="0"
	 */
	protected Integer numberOfFilesToGenerate;
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		ContentGeneratorService contentGeneratorService = ContentGeneratorService.getInstance();
		ExportBO export = super.initExport();
		export.setNumberOfFilesToGenerate(numberOfFilesToGenerate);
		contentGeneratorService.generateFiles(export);
	}
}
