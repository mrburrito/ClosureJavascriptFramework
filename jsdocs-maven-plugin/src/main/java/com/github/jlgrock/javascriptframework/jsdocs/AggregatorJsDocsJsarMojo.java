package com.github.jlgrock.javascriptframework.jsdocs;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.github.jlgrock.javascriptframework.mavenutils.logging.MojoLogAppender;

/**
 * Generates javascript docs from the jsdoc-toolkit (the final version) and
 * stores them into a js archive.
 * 
 * @goal aggregate-jsar
 * 
 */
public class AggregatorJsDocsJsarMojo extends AbstractJsDocsNonAggMojo {
	/**
	 * The Logger.
	 */
	private static final Logger LOGGER = Logger
			.getLogger(AggregatorJsDocsJsarMojo.class);

	/**
	 * Specifies the directory where the generated jar file will be put.
	 * 
	 * @parameter default-value="${project.build.directory}"
	 */
	private File jsarOutputDirectory;

	@Override
	public final File getArchiveOutputDirectory() {
		return jsarOutputDirectory;
	}

	@Override
	public final void execute() throws MojoExecutionException,
			MojoFailureException {
		LOGGER.debug("starting report execution...");
		MojoLogAppender.beginLogging(this);
		try {
			ReportGenerator
					.extractJSDocToolkit(getToolkitExtractDirectory());
			Set<File> sourceFiles = getSourceFiles();
			List<String> args = createArgumentStack(sourceFiles);
			ReportGenerator.executeJSDocToolkit(args, getToolkitExtractDirectory());
			File innerDestDir = getArchiveOutputDirectory();
			if (innerDestDir.exists()) {
				AbstractJsDocsMojo.generateArchive(this, innerDestDir,
						getFinalName() + "-" + getClassifier() + ".jsar");
			}
		} catch (Exception e) {
			LOGGER.error("There was an error in the execution of the report: "
					+ e.getMessage(), e);
			throw new MojoExecutionException(e.getMessage(), e);
		} finally {
			MojoLogAppender.endLogging();
		}
	}

	@Override
	public final String getClassifier() {
		return "jsdocs";
	}

}