package org.jahia.utils.maven.plugin.contentgenerator.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jahia.utils.maven.plugin.contentgenerator.ContentGeneratorService;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;

import java.io.IOException;

/**
 * @goal generate-pages
 * @requiresProject false
 * @author Guillaume Lucazeau
 * 
 */
public class GeneratePagesMojo extends ContentGeneratorMojo {
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		ContentGeneratorService contentGeneratorService = ContentGeneratorService.getInstance();
		ExportBO export = super.initExport();

		getLog().info("Jahia content generator starts");
		getLog().info(export.getTotalPages() + " pages will be created");
		
		try {
			contentGeneratorService.generatePages(export);
		} catch (IOException e) {
			getLog().error("Error while writing to output file: ", e);
		}
		
		getLog().info("XML import file available here: " + export.getOutputFile().getAbsolutePath());
		if (export.getCreateMap()) {
			getLog().info("Paths list available here: " + export.getMapFile().getAbsolutePath());
		}

		getLog().info("Completed, " + export.getTotalPages() + " pages created");
		getLog().info("Please remember that your export contains articles randomly picked from Wikipedia data, and the content generator is not responsible for the articles content. Thank you!");

	}
}
