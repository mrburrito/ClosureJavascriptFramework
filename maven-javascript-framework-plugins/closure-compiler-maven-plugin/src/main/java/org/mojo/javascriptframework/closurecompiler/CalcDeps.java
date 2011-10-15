package org.mojo.javascriptframework.closurecompiler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Represents a dependency that is used to build and walk a tree.  This is a direct
 * port from the google python script.
 *
 */
public class CalcDeps {
	private static final Logger logger = Logger.getLogger( CalcDeps.class );
	
	/**
	 * Build a list of dependencies from a list of files.
	 * Takes a list of files, extracts their provides and requires, and builds
	 * out a list of dependency objects.
	 * 
	 * @param files a list of files to be parsed for goog.provides and goog.requires.
	 * @throws IOException 
	 * @returns A list of dependency objects, one for each file in the files argument.
	 */
	private static HashMap<File, DependencyInfo> buildDependenciesFromFiles(Collection<File> files) throws IOException {
		HashMap<File, DependencyInfo> result = new HashMap<File, DependencyInfo>();
		Set<File> searchedAlready = new HashSet<File>();
		for (File file:files) {
			if (!searchedAlready.contains(file)) {
				DependencyInfo dep = AnnotationFileReader.parseForDependencyInfo(file);
				result.put(file, dep);
				searchedAlready.add(file);
			}
		}
		return result;
	}
	
	/**
	 * Calculates the dependencies for given inputs.
	 * 
	 * This method takes a list of paths (files, directories) and builds a 
	 * searchable data structure based on the namespaces that each .js file
	 * provides.  It then parses through each input, resolving dependencies
	 * against this data structure.  The final output is a list of files,
	 * including the inputs, that represent all of the code that is needed to
	 * compile the given inputs.
	 *     
	 * @param paths the references (files, directories) that are used to build the
	 * dependency hash.
	 * @param inputs the inputs (files, directories, namespaces) that have dependencies
	 * that need to be calculated.
	 * @return A list of all files, including inputs, that are needed to compile the 
	 * given inputs.
	 * @throws MojoExecutionException 
	 * @throws Exception if a provided input is invalid.
	 */
	private static List<DependencyInfo> calculateDependencies(final File base_js, final Set<File> inputs, final Set<File> paths) throws IOException {
		HashSet<File> temp = new HashSet<File>();
		temp.addAll(inputs);
		HashMap<File, DependencyInfo> input_hash = buildDependenciesFromFiles(inputs);
		HashMap<File, DependencyInfo> search_hash = buildDependenciesFromFiles(paths);
		logger.info("Dependencies Calculated.");
    	
		List<DependencyInfo> sortedDeps = slowSort(input_hash.values(), search_hash.values());
		logger.info("Dependencies Sorted.");
    	
    	return sortedDeps;
	}
	
	/**
	 * Print out a deps.js file from a list of source paths.
	 * 
	 * @param source_paths: Paths that we should generate dependency info for.
	 * @param deps: Paths that provide dependency info. Their dependency info should
	 * not appear in the deps file.
	 * @param out: The output file.
	 * @throws Exception 
	 * @returns True on success, false if it was unable to find the base path 
	 * to generate deps relative to.
	 */
	private static boolean outputDeps(final List<DependencyInfo> sortedDeps, final File outputFile) throws IOException {
		FileWriter fw = new FileWriter(outputFile);
		BufferedWriter buff = new BufferedWriter(fw);
		
		buff.append("\n// This file was autogenerated by CalcDeps.java\n");
		for (DependencyInfo file_dep : sortedDeps) {
			if (file_dep != null) {
				buff.write(file_dep.toString(outputFile));
				buff.write("\n");
				buff.flush();
			}
		}
		logger.info("Deps file written.");

		return true;
		
	}
	
	/**
	 * Compare every element to one another.  This is significantly slower than a merge sort, but 
	 * guarantees that deps end up in the right order
	 * 
	 * @param deps
	 */
	private static List<DependencyInfo> slowSort(final Collection<DependencyInfo> inputs, final Collection<DependencyInfo> deps) {
		HashMap<String, DependencyInfo> search_set = buildSearchList(deps);
		HashSet<File> seenList = new HashSet<File>();
		ArrayList<DependencyInfo> result_list = new ArrayList<DependencyInfo>();
		for (DependencyInfo input : inputs) {
			if (!seenList.contains(input.getFile())) {
				seenList.add(input.getFile());
				for (String require : input.getRequires()) {
					orderDependenciesForNamespace(require, search_set, seenList, result_list);
				}
				result_list.add(input);
			}			
		}
		return result_list;
	}

	
	private static void orderDependenciesForNamespace(String require_namespace, HashMap<String, DependencyInfo> search_set, HashSet<File> seenList, ArrayList<DependencyInfo> result_list) {
		if (!search_set.containsKey(require_namespace)) {
			//throw exception
			logger.error("search set doesn't contain key '" + require_namespace + "'");
		}
		DependencyInfo dep = search_set.get(require_namespace);
		if (!seenList.contains(dep.getFile())) {
			seenList.add(dep.getFile());
			for (String sub_require : dep.getRequires()) {
				orderDependenciesForNamespace(sub_require, search_set, seenList, result_list);
			}
			result_list.add(dep);
		}
	}

	private static HashMap<String, DependencyInfo> buildSearchList(Collection<DependencyInfo> deps) {
		HashMap<String, DependencyInfo> returnVal = new HashMap<String, DependencyInfo>();
		for (DependencyInfo dep : deps) {
			for (String provide : dep.getProvides()) {
				returnVal.put(provide, dep);
			}
		}
		return returnVal;
	}
	
	private static List<File> pullFilesFromDeps(List<DependencyInfo> sortedDeps) {
		ArrayList<File> returnVal = new ArrayList<File>();
		for (DependencyInfo dep : sortedDeps) {
			returnVal.add(dep.getFile());
		}
		return returnVal;
	}

	public static List<File> executeCalcDeps(final File googleBaseFile, final Set<File> inputs, final Set<File> paths, final File outputFile) throws IOException {
		logger.debug("Finding Closure dependencies...");
		List<DependencyInfo> sortedDeps = calculateDependencies(googleBaseFile, inputs, paths);
		
		//create deps file
		logger.debug("Outputting Closure dependency file...");
		outputDeps(sortedDeps, outputFile);
		
		logger.debug("Closure dependencies created");
		return pullFilesFromDeps(sortedDeps);
	}

}
