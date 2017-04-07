package org.ihtsdo.changeanalyzer;

import java.io.File;

import org.apache.commons.configuration.XMLConfiguration;

public class ReleaseDiffReportRunner {

	private static final String CURRENT_RELEASE_DATE = "currentReleaseDate";
	private static final String START_DATE = "startDate";
	private static final String DIFF_REPORT_OUTPUT_DIR = "diffReportOutputDirectory";
	private static final String INPUT_FULL_DIR = "inputFullFileDirectory";

	public static void main(String[] args) throws Exception {
		if (args == null || args.length < 1 ) {
			
			throw new RuntimeException("Please pass in the config.xml with parameters for input folder,output folder,previous release date, current release date!");
		}
		String configFilename = args[0];
		XMLConfiguration xmlConfig = new XMLConfiguration(configFilename);
		String inputDir = xmlConfig.getString(INPUT_FULL_DIR);
		String outputDir = xmlConfig.getString(DIFF_REPORT_OUTPUT_DIR);
		String startDate = xmlConfig.getString(START_DATE);
		String endDate = xmlConfig.getString(CURRENT_RELEASE_DATE);
		
		ReleaseFilesReportPlugin report = new ReleaseFilesReportPlugin();
		report.setInputDirectory(new File(inputDir));
		report.setOutputDirectory(new File(outputDir));
		report.setStartDate(startDate);
		report.setEndDate(endDate);
		report.setReleaseDate(endDate);
		report.execute();
	}

}
