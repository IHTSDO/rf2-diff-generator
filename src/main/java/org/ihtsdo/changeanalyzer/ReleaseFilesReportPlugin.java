package org.ihtsdo.changeanalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.changeanalyzer.data.Rf2AssociationRefsetRow;
import org.ihtsdo.changeanalyzer.data.Rf2AttributeValueRefsetRow;
import org.ihtsdo.changeanalyzer.data.Rf2DescriptionRow;
import org.ihtsdo.changeanalyzer.data.Rf2LanguageRefsetRow;
import org.ihtsdo.changeanalyzer.data.Rf2RelationshipRow;
import org.ihtsdo.changeanalyzer.file.Rf2AssociationRefsetFile;
import org.ihtsdo.changeanalyzer.file.Rf2AttributeValueRefsetFile;
import org.ihtsdo.changeanalyzer.file.Rf2ConceptFile;
import org.ihtsdo.changeanalyzer.file.Rf2DescriptionFile;
import org.ihtsdo.changeanalyzer.file.Rf2LanguageRefsetFile;
import org.ihtsdo.changeanalyzer.file.Rf2RelationshipFile;
import org.ihtsdo.changeanalyzer.model.ChangeSummary;
import org.ihtsdo.changeanalyzer.model.Concept;
import org.ihtsdo.changeanalyzer.model.Description;
import org.ihtsdo.changeanalyzer.model.FileChangeReport;
import org.ihtsdo.changeanalyzer.model.Relationship;
import org.ihtsdo.changeanalyzer.model.RetiredConcept;
import org.ihtsdo.changeanalyzer.utils.FileHelper;

import com.google.gson.Gson;

/**
 * @goal report-differences
 * @phase install
 */
public class ReleaseFilesReportPlugin extends AbstractMojo {

	private static final String SUMMARY_FILE = "diff_index.json";
	//Currently shown in the dailybuild browser
	public static final String NEW_CONCEPTS_FILE = "new_concepts.json";
	
	public static final String RETIRED_CONCEPT_REASON_FILE = "inactivated_concept_reason.json";
	
	private static final String REACTIVATED_CONCEPTS_REPORT = "reactivated_concepts.json";

	//changed FSNs
	private static final String CHANGED_FSN="changed_fsn.json";
	private static final String RETIRED_DESCRIPTIONS_FILE = "inactivated_descriptions.json";
	public static final String NEW_DESCRIPTIONS_FILE = "new_descriptions.json";
	private static final String REACTIVATED_DESCRIPTIONS_FILE = "reactivated_descriptions.json";
	private static final String DEFINED_CONCEPTS_REPORT = "defined_concepts.json";
	public static final String NEW_RELATIONSHIPS_FILE = "new_relationships.json";
	private static final String SYN_ACCEPTABILITY_CHANGED = "acceptability_changed_on_synonym.json";

	//Not shown yet
	public static final String OLD_CONCEPTS_NEW_DESCRIPTIONS_FILE = "old_concepts_new_descriptions.json";

	public static final String OLD_CONCEPTS_NEW_RELATIONSHIPS_FILE = "old_concepts_new_relationships.json";

	public static final String NEW_INACTIVE_CONCEPTS_FILE = "new_inactive_concepts.json";

	public static final String REL_GROUP_CHANGED_FILE = "rel_group_changed_relationships.json";

	private static final Logger logger = Logger.getLogger(ReleaseFilesReportPlugin.class);

	private static final String PRIMITIVE_CONCEPTS_REPORT = "primitive_concepts.json";
	
	private static final String TARGET_POINTER_TO_CHANGED_SOURCE_DESCRIPTION = "active_language_references_to_now_inactive_descriptions.json";
	
	private static final String DEFAULT_EDITION = "International";

    private String sep = System.getProperty("line.separator");
    
    private Gson gson = new Gson();
	/**
	 * Location of the directory of report files
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	private File outputDirectory;

	/**
	 * Location of the directory of the release folder.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	private File inputDirectory;

	public File getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public File getInputDirectory() {
		return inputDirectory;
	}

	public void setInputDirectory(File inputDirectory) {
		this.inputDirectory = inputDirectory;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}
	
	public String getEditionName() {
		if(editionName == null || editionName.isEmpty() ){
			setEditionName(DEFAULT_EDITION);
		}
		return editionName;
	}

	public void setEditionName(String editionName) {
		this.editionName = editionName;
	}


	/**
	 * Start date for the reports.
	 * 
	 * @parameter
	 * @required
	 */
	private String startDate;

	/**
	 * End date for the reports.
	 * 
	 * @parameter
	 * @required
	 */
	private String endDate;

	/**
	 * Release date. in case there is more than one release in the release
	 * folder.
	 * 
	 * @parameter
	 */
	private String releaseDate;
	
	/**
	 * Edition name. Default to International if not set
	 * 
	 * @parameter
	 */
	private String editionName;

	/**
	 * Target Language File
	 * 
	 * @parameter
	 */
	private File targetLanguage;

	private ChangeSummary changeSummary;

	private boolean langTargetCtrl;

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			if (!outputDirectory.exists()) {
				outputDirectory.mkdirs();
			}
			changeSummary=new ChangeSummary();
			logger.info("Loading descriptions");
			Rf2DescriptionFile rf2DescFile = new Rf2DescriptionFile(getFilePath(ReleaseFileType.DESCRIPTION));
			logger.info("Loading concepts");
			Rf2ConceptFile conceptFile = new Rf2ConceptFile(getFilePath(ReleaseFileType.CONCEPT));
			logger.info("Loading attribute value refset");
			Rf2AttributeValueRefsetFile attrValue = new Rf2AttributeValueRefsetFile(getFilePath(ReleaseFileType.ATTRIBUTE_VALUE_REFSET));
			logger.info("Loading association value refset");
			Rf2AssociationRefsetFile associationFile = new Rf2AssociationRefsetFile(getFilePath(ReleaseFileType.ASSOCIATION_REFSET));
			ArrayList<Long> newConcepts = generateNewConceptsReport(rf2DescFile, conceptFile);
			logger.info("Total new concepts:" + newConcepts.size());
			ArrayList<Long> retiredConcepts=generatingRetiredConceptReasons(rf2DescFile, conceptFile, attrValue, associationFile);
			logger.info("Total inactive concepts:" + retiredConcepts.size());
			ArrayList<Long> conceptsReactivated = reactivatedConceptsReport(rf2DescFile, conceptFile);
			logger.info("Total reactivated concepts:" + conceptsReactivated.size());
			generateReportForDescriptionChanges(rf2DescFile);
			saveSummary();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Failed to genereate release report due to:",e);
		}

	}
	
	
	private void generateReportForDescriptionChanges(Rf2DescriptionFile rf2DescFile) throws Exception {
		generatingChangedFSN(rf2DescFile);
		generateRetiredDescriptionsReport(rf2DescFile);
		generatingExistingConceptsNewDescriptions(rf2DescFile);
		generateReactivatedDescriptionsReport(rf2DescFile);
	}

	private ArrayList<Long> generateTargetDescriptionPointerToSource(
			Rf2DescriptionFile rf2DescFile,
			Rf2LanguageRefsetFile targetLangFile,
			ArrayList<Long> repcomponents) throws Exception {
		FileOutputStream fos;
		OutputStreamWriter osw;
		BufferedWriter bw;
		fos = new FileOutputStream(new File(outputDirectory, TARGET_POINTER_TO_CHANGED_SOURCE_DESCRIPTION));
		logger.info("Generating " + TARGET_POINTER_TO_CHANGED_SOURCE_DESCRIPTION);
		osw = new OutputStreamWriter(fos, "UTF-8");
		bw = new BufferedWriter(osw);
		ArrayList<Long> changedDesc = rf2DescFile.getChangedComponentIds(startDate, endDate);
		int count=0;
		boolean bPrim=true;
		bw.append("[");
		for (Long long1 : changedDesc) {
			Rf2LanguageRefsetRow langRow = targetLangFile.getLastActiveRow(startDate,long1.toString());
			if (langRow!=null && langRow.getActive()==1){
				Rf2DescriptionRow rf2DescRow = rf2DescFile.getLastActiveRow(startDate,long1);
				if (!repcomponents.contains(rf2DescRow.getConceptId()) &&
						rf2DescRow.getActive()==0) {
					repcomponents.add(rf2DescRow.getConceptId());
					if (!bPrim){
						bw.append(",");
					}else{
						bPrim=false;
					}
					
					Description desc=new Description(long1.toString(), rf2DescRow.getEffectiveTime() , String.valueOf(rf2DescRow.getActive()) , rf2DescRow.getConceptId().toString() ,
							rf2DescRow.getLanguageCode() , rf2DescFile.getFsn(rf2DescRow.getTypeId()) , rf2DescRow.getTerm() ,
						    rf2DescFile.getFsn(rf2DescRow.getCaseSignificanceId()));
					bw.append(gson.toJson(desc).toString());
					desc=null;
					bw.append(sep);
					count++;
				}
			}
		}
		bw.append("]");
		bw.close();

		addFileChangeReport(TARGET_POINTER_TO_CHANGED_SOURCE_DESCRIPTION,count,"Active language references to now inactive descriptions.");
		
		return repcomponents;		
	}

	private ArrayList<Long> generateDescriptionAcceptabilityChanges(
			Rf2LanguageRefsetFile sourceLangFile, Rf2DescriptionFile rf2DescFile,ArrayList<Long> repcomponents) throws Exception {
		FileOutputStream fos;
		OutputStreamWriter osw;
		BufferedWriter bw;
		fos = new FileOutputStream(new File(outputDirectory, SYN_ACCEPTABILITY_CHANGED));
		logger.info("Generating " + SYN_ACCEPTABILITY_CHANGED);
		osw = new OutputStreamWriter(fos, "UTF-8");
		bw = new BufferedWriter(osw);
		ArrayList<String> changedLang = sourceLangFile.getAcceptabilityIdChanged(startDate, endDate);
		int count=0;
		boolean bPrim=true;
		bw.append("[");
		for (String string : changedLang) {
			Rf2DescriptionRow rf2DescRow = rf2DescFile.getLastActiveRow(startDate, Long.parseLong(string));
				if (!repcomponents.contains(rf2DescRow.getConceptId()) 
						&& rf2DescRow.getTypeId()!=900000000000003001L) {
					repcomponents.add(rf2DescRow.getConceptId());
					if (!bPrim){
						bw.append(",");
					}else{
						bPrim=false;
					}
					Description desc=new Description(string, rf2DescRow.getEffectiveTime() , String.valueOf(rf2DescRow.getActive()) , rf2DescRow.getConceptId().toString() ,
							rf2DescRow.getLanguageCode() , rf2DescFile.getFsn(rf2DescRow.getTypeId()) , rf2DescRow.getTerm() ,
						    rf2DescFile.getFsn(rf2DescRow.getCaseSignificanceId()));
					bw.append(gson.toJson(desc).toString());
					desc=null;
					bw.append(sep);
					count++;
				}
		}
		bw.append("]");
		bw.close();

		addFileChangeReport(SYN_ACCEPTABILITY_CHANGED,count,"Acceptability changed in descriptions (no FSN)");
		
		return repcomponents;
	}

	private void saveSummary() throws IOException {
		
		changeSummary.setTitle("Changes in "+ editionName" Edition " +  endDate + " Development Path since " + startDate + " "+ editionName" Release");
		Date now=new Date();
		changeSummary.setExecutionTime(now.toString());
		changeSummary.setFrom(startDate);
		changeSummary.setTo(endDate);
		FileOutputStream fos;
		OutputStreamWriter osw;
		BufferedWriter bw;
		fos = new FileOutputStream(new File(outputDirectory, SUMMARY_FILE));
		logger.info("Generating diff_index.json");
		osw = new OutputStreamWriter(fos, "UTF-8");
		bw = new BufferedWriter(osw);
		
		bw.append(gson.toJson(changeSummary).toString());
		bw.append(sep);
		
		bw.close();
		System.gc();
		
	}

	private String getFilePath(ReleaseFileType fileType) {
		String result = null;
		switch (fileType) {
		case DESCRIPTION:
			//add sct2 for description to differentiate from the descriptionType refset file
			result = getFilePathRecursive(inputDirectory, "sct2_Description");
			break;
		case CONCEPT:
			result = getFilePathRecursive(inputDirectory, "sct2_Concept");
			break;
		case RELATIONSHIP:
			result = getFilePathRecursive(inputDirectory, "sct2_Relationship");
			break;
		case ASSOCIATION_REFSET:
			result = getFilePathRecursive(inputDirectory, "der2_cRefset_AssociationReference");
			break;
		case ATTRIBUTE_VALUE_REFSET:
			result = getFilePathRecursive(inputDirectory, "der2_cRefset_AttributeValue");
			break;
		case LANGUAGE_REFSET:
			result = getFilePathRecursive(inputDirectory, "der2_cRefset_Language");
			break;
		default:
			break;
		}
		if (result == null) {
			logger.error("No file is found for release file type:" + fileType.toString() + " in folder:" + inputDirectory);
		}
		return result;
	}

	public String getFilePathRecursive(File folder, String namePart) {
		String result = null;
		if (folder.isDirectory()) {
			File[] files = folder.listFiles();
			int i = 0;
			while (i < files.length && result == null) {
				result = getFilePathRecursive(files[i], namePart);
				i++;
			}
		} else {
			if (folder.getName().contains(namePart)) {
				if (releaseDate != null && !releaseDate.equals("") && folder.getName().contains(releaseDate)) {
					result = folder.getPath();
				} else if (releaseDate == null || releaseDate.equals("")) {
					result = folder.getPath();
				}
			}
		}
		return result;
	}

	private void generateRelGroupChangedRelationships(Rf2DescriptionFile rf2DescFile, Rf2RelationshipFile relFile, String startDate, String endDate) throws Exception {
		FileOutputStream fos;
		OutputStreamWriter osw;
		BufferedWriter bw;
		ArrayList<Long> newRels = relFile.getRelGroupChanged(startDate, endDate);
		fos = new FileOutputStream(new File(outputDirectory, REL_GROUP_CHANGED_FILE));
		logger.info("Generating rel_group_changed_relationships.json");
		osw = new OutputStreamWriter(fos, "UTF-8");
		bw = new BufferedWriter(osw);
		int count=0;
		boolean bPrim=true;
		bw.append("[");
		for (Long long1 : newRels) {
			if (!bPrim){
				bw.append(",");
			}else{
				bPrim=false;
			}
			Rf2RelationshipRow row = relFile.getById(long1, startDate);
			Relationship rel=new Relationship(row.getId().toString() , String.valueOf(row.getActive()) , rf2DescFile.getFsn(row.getSourceId()),row.getSourceId().toString(), rf2DescFile.getFsn(row.getDestinationId()) ,
					 rf2DescFile.getFsn(row.getTypeId()) , rf2DescFile.getFsn(row.getCharacteristicTypeId()));
				bw.append(gson.toJson(rel).toString());
				rel=null;
				bw.append(sep);
				count++;
		}
		bw.append("]");
		bw.close();

		addFileChangeReport(REL_GROUP_CHANGED_FILE,count,"Relationships group number changed");
		
	}

	private void generatingInactiveConcepts(Rf2DescriptionFile rf2DescFile, Rf2ConceptFile conceptFile) throws FileNotFoundException, UnsupportedEncodingException, IOException {
		FileOutputStream fos;
		OutputStreamWriter osw;
		BufferedWriter bw;
		fos = new FileOutputStream(new File(outputDirectory, NEW_INACTIVE_CONCEPTS_FILE));
		osw = new OutputStreamWriter(fos, "UTF-8");
		bw = new BufferedWriter(osw);
		ArrayList<Long> newInactive = conceptFile.getNewInactiveComponentIds(startDate);
		logger.info("Total new inactive concepts:" + newInactive.size());
		generateConceptReport(rf2DescFile, conceptFile, bw, newInactive);
		addFileChangeReport(NEW_INACTIVE_CONCEPTS_FILE,newInactive.size(),"New inactive concepts");
	}

	private void generateConceptReport(Rf2DescriptionFile rf2DescFile, Rf2ConceptFile conceptFile, BufferedWriter bw, ArrayList<Long> newInactive) throws IOException {
		boolean bPrim=true;
		bw.append("[");
		for (Long long1 : newInactive) {
			if (!bPrim){
				bw.append(",");
			}else{
				bPrim=false;
			}
			String fsn = rf2DescFile.getFsn(long1);
			Pattern p = Pattern.compile("\\((.*?)\\)", Pattern.DOTALL);
			String semanticTag = "";
			if (fsn != null) {
				Matcher matcher = p.matcher(fsn);
				while (matcher.find()) {
					semanticTag = matcher.group(1);
				}
			}
			Concept concept=new Concept(long1.toString() ,rf2DescFile.getFsn(conceptFile.getDefinitionStatusId(long1)) , fsn , semanticTag);
			bw.append(gson.toJson(concept).toString());
			concept=null;
			bw.append(sep);
		}
		bw.append("]");
		bw.close();
	}

	private void generatingDefinedConceptsReport(Rf2DescriptionFile rf2DescFile, Rf2ConceptFile conceptFile) throws FileNotFoundException, UnsupportedEncodingException, IOException {
		FileOutputStream fos;
		OutputStreamWriter osw;
		BufferedWriter bw;
		fos = new FileOutputStream(new File(outputDirectory, DEFINED_CONCEPTS_REPORT));
		osw = new OutputStreamWriter(fos, "UTF-8");
		bw = new BufferedWriter(osw);
		ArrayList<Long> newInactive = conceptFile.getDefinedConectps(startDate, endDate);
		generateConceptReport(rf2DescFile, conceptFile, bw, newInactive);

		addFileChangeReport(DEFINED_CONCEPTS_REPORT,newInactive.size(),"New fully defined concepts");
		
	}

	private ArrayList<Long> reactivatedConceptsReport(Rf2DescriptionFile rf2DescFile, Rf2ConceptFile conceptFile) throws FileNotFoundException, UnsupportedEncodingException, IOException {
		FileOutputStream fos;
		OutputStreamWriter osw;
		BufferedWriter bw;
		fos = new FileOutputStream(new File(outputDirectory, REACTIVATED_CONCEPTS_REPORT));
		logger.info("Generating " + REACTIVATED_CONCEPTS_REPORT);
		osw = new OutputStreamWriter(fos, "UTF-8");
		bw = new BufferedWriter(osw);
		ArrayList<Long> reactConcepts = conceptFile.getReactivatedComponents(startDate, endDate);
		
		generateConceptReport(rf2DescFile, conceptFile, bw, reactConcepts);

		addFileChangeReport(REACTIVATED_CONCEPTS_REPORT,reactConcepts.size(),"Reactivated concepts");
		
		return reactConcepts;
	}

	private void generatingPrimitiveConceptsReport(Rf2DescriptionFile rf2DescFile, Rf2ConceptFile conceptFile) throws FileNotFoundException, UnsupportedEncodingException, IOException {
		FileOutputStream fos;
		OutputStreamWriter osw;
		BufferedWriter bw;
		fos = new FileOutputStream(new File(outputDirectory, PRIMITIVE_CONCEPTS_REPORT));
		osw = new OutputStreamWriter(fos, "UTF-8");
		bw = new BufferedWriter(osw);
		ArrayList<Long> newInactive = conceptFile.getPrimitivatedConectps(startDate, endDate);
		generateConceptReport(rf2DescFile, conceptFile, bw, newInactive);

		addFileChangeReport(PRIMITIVE_CONCEPTS_REPORT,newInactive.size(),"New primitive concepts");
	}

	private void generateOldConceptsNewRelationships(Rf2DescriptionFile rf2DescFile, Rf2RelationshipFile relFile, ArrayList<Long> newcomponents) throws FileNotFoundException,
            UnsupportedEncodingException, IOException {
		FileOutputStream fos;
		OutputStreamWriter osw;
		BufferedWriter bw;
		fos = new FileOutputStream(new File(outputDirectory, OLD_CONCEPTS_NEW_RELATIONSHIPS_FILE));
		logger.info("Generating old_concepts_new_relationships.json");
		osw = new OutputStreamWriter(fos, "UTF-8");
		bw = new BufferedWriter(osw);
		ArrayList<Long> existingRels = relFile.getExistingComponentIds(startDate);
		int count=0;
		boolean bPrim=true;
		bw.append("[");
		for (Long long1 : existingRels) {
			ArrayList<Rf2RelationshipRow> rf2RelRows = relFile.getAllRows(startDate, long1);
			for (Rf2RelationshipRow row : rf2RelRows) {
				if (!newcomponents.contains(Long.parseLong(row.getSourceId().toString()))) {
					if (!bPrim){
						bw.append(",");
					}else{
						bPrim=false;
					}
					Relationship rel=new Relationship(row.getId().toString() , String.valueOf(row.getActive()) , rf2DescFile.getFsn(row.getSourceId()),row.getSourceId().toString(), rf2DescFile.getFsn(row.getDestinationId()) ,
							 rf2DescFile.getFsn(row.getTypeId()) , rf2DescFile.getFsn(row.getCharacteristicTypeId()));
						bw.append(gson.toJson(rel).toString());
						rel=null;
						bw.append(sep);
						count++;
				}
			}
		}
		bw.append("]");
		bw.close();

		addFileChangeReport(OLD_CONCEPTS_NEW_RELATIONSHIPS_FILE,count,"New relationships in existing concepts");
		
	}
	
	private ArrayList<Long> generatingExistingConceptsNewDescriptions(Rf2DescriptionFile rf2DescFile) throws FileNotFoundException, UnsupportedEncodingException, IOException {
		FileOutputStream fos;
		OutputStreamWriter osw;
		BufferedWriter bw;
		fos = new FileOutputStream(new File(outputDirectory, OLD_CONCEPTS_NEW_DESCRIPTIONS_FILE));
		logger.info("Generating " + OLD_CONCEPTS_NEW_DESCRIPTIONS_FILE);
		osw = new OutputStreamWriter(fos, "UTF-8");
		bw = new BufferedWriter(osw);
		ArrayList<Long> newDescriptions = rf2DescFile.getNewComponentIds(startDate);
		ArrayList<Long> repcomponents = new ArrayList<Long>();
		int count=0;
		boolean bPrim=true;
		bw.append("[");
		for (Long long1 : newDescriptions) {
			ArrayList<Rf2DescriptionRow> rf2DescRows = rf2DescFile.getAllRows(startDate, long1);
			for (Rf2DescriptionRow rf2DescRow : rf2DescRows) {
				if (!repcomponents.contains(rf2DescRow.getConceptId()) 
						&& rf2DescRow.getTypeId()!=900000000000003001L) {
					repcomponents.add(rf2DescRow.getConceptId());
					if (!bPrim){
						bw.append(",");
					}else{
						bPrim=false;
					}
					Description desc=new Description(long1.toString() , rf2DescRow.getEffectiveTime() , String.valueOf(rf2DescRow.getActive()) , rf2DescRow.getConceptId().toString() ,
							rf2DescRow.getLanguageCode() , rf2DescFile.getFsn(rf2DescRow.getTypeId()) , rf2DescRow.getTerm() ,
						    rf2DescFile.getFsn(rf2DescRow.getCaseSignificanceId()));
					bw.append(gson.toJson(desc).toString());
					desc=null;
					bw.append(sep);
					count++;
				}
			}
		}
		bw.append("]");
		bw.close();

		addFileChangeReport(OLD_CONCEPTS_NEW_DESCRIPTIONS_FILE,count,"New descriptions (synonyms only) for existing concepts");
		
		return repcomponents;
	}
	
	private ArrayList<Long> generatingChangedFSN(Rf2DescriptionFile rf2DescFile) throws FileNotFoundException, UnsupportedEncodingException, IOException {
		FileOutputStream fos;
		OutputStreamWriter osw;
		BufferedWriter bw;
		fos = new FileOutputStream(new File(outputDirectory, CHANGED_FSN));
		logger.info("Generating " + CHANGED_FSN);
		osw = new OutputStreamWriter(fos, "UTF-8");
		bw = new BufferedWriter(osw);
		ArrayList<Long> descriptions = rf2DescFile.getChangedComponentIds(startDate, endDate);
		ArrayList<Long> repcomponents = new ArrayList<Long>();
		int count=0;
		boolean bPrim=true;
		bw.append("[");
		for (Long long1 : descriptions) {
			ArrayList<Rf2DescriptionRow> rf2DescRows = rf2DescFile.getAllRows(startDate, long1);
			for (Rf2DescriptionRow rf2DescRow : rf2DescRows) {
				if (!repcomponents.contains(rf2DescRow.getConceptId()) 
						&& rf2DescRow.getActive()==1 
						&& rf2DescRow.getTypeId()==900000000000003001L ) {
					repcomponents.add(rf2DescRow.getConceptId());
					if (!bPrim){
						bw.append(",");
					}else{
						bPrim=false;
					}
					Description desc=new Description(long1.toString() , rf2DescRow.getEffectiveTime() , String.valueOf(rf2DescRow.getActive()) , rf2DescRow.getConceptId().toString() ,
							rf2DescRow.getLanguageCode() , rf2DescFile.getFsn(rf2DescRow.getTypeId()) , rf2DescRow.getTerm() ,
						    rf2DescFile.getFsn(rf2DescRow.getCaseSignificanceId()));
					bw.append(gson.toJson(desc).toString());
					desc=null;
					bw.append(sep);
					count++;
				}
			}
		}
		bw.append("]");
		bw.close();

		addFileChangeReport(CHANGED_FSN,count,"Changed FSNs");
		return repcomponents;
	}

	private ArrayList<Long> generateRetiredDescriptionsReport(Rf2DescriptionFile rf2DescFile) throws FileNotFoundException, UnsupportedEncodingException, IOException {
		FileOutputStream fos;
		OutputStreamWriter osw;
		BufferedWriter bw;
		fos = new FileOutputStream(new File(outputDirectory, RETIRED_DESCRIPTIONS_FILE));
		logger.info("Generating " + RETIRED_DESCRIPTIONS_FILE);
		osw = new OutputStreamWriter(fos, "UTF-8");
		bw = new BufferedWriter(osw);
		ArrayList<Long> retiredDescriptions = rf2DescFile.getRetiredComponents(startDate, endDate);
		ArrayList<Long> filteredRetDesc=new ArrayList<Long>();
		ArrayList<Long> repcomponents = new ArrayList<Long>();
		for(Long retiredDesc:retiredDescriptions){

			ArrayList<Rf2DescriptionRow> rf2DescRows = rf2DescFile.getAllRows(startDate, retiredDesc);
			for (Rf2DescriptionRow rf2DescRow : rf2DescRows) {
				if (!repcomponents.contains(rf2DescRow.getConceptId())
						&& rf2DescRow.getActive()==0 
						&& rf2DescRow.getTypeId()!=900000000000003001L ) {
					filteredRetDesc.add(retiredDesc);
					repcomponents.add(rf2DescRow.getConceptId());
				}
			}
		}
		int count=writeDescriptionsFile(rf2DescFile, bw, filteredRetDesc);

		addFileChangeReport(RETIRED_DESCRIPTIONS_FILE,count,"Inactivated descriptions(synonyms only)");
		
		return repcomponents;
	}

	private ArrayList<Long> generateReactivatedDescriptionsReport(Rf2DescriptionFile rf2DescFile) throws FileNotFoundException, UnsupportedEncodingException, IOException {
		FileOutputStream fos;
		OutputStreamWriter osw;
		BufferedWriter bw;
		fos = new FileOutputStream(new File(outputDirectory, REACTIVATED_DESCRIPTIONS_FILE));
		logger.info("Generating " + REACTIVATED_DESCRIPTIONS_FILE);
		osw = new OutputStreamWriter(fos, "UTF-8");
		bw = new BufferedWriter(osw);
		ArrayList<Long> reactivedDescriptions = rf2DescFile.getReactivatedComponents(startDate, endDate);
		ArrayList<Long> filteredReactDesc=new ArrayList<Long>();
		ArrayList<Long> repcomponents = new ArrayList<Long>();
		for(Long retiredDesc:reactivedDescriptions){
			ArrayList<Rf2DescriptionRow> rf2DescRows = rf2DescFile.getAllRows(startDate, retiredDesc);
			for (Rf2DescriptionRow rf2DescRow : rf2DescRows) {
				if (!repcomponents.contains(rf2DescRow.getConceptId())
						&& rf2DescRow.getActive()==1 && rf2DescRow.getTypeId()!=900000000000003001L ) {
					repcomponents.add(rf2DescRow.getConceptId());
					filteredReactDesc.add(retiredDesc);
				}
			}
		}
		int count=writeDescriptionsFile(rf2DescFile,  bw, filteredReactDesc);
		
		addFileChangeReport(REACTIVATED_DESCRIPTIONS_FILE,count,"Reactivated descriptions(synonyms only)");
		
		return repcomponents;
	}

	private int writeDescriptionsFile(Rf2DescriptionFile rf2DescFile,  BufferedWriter bw, ArrayList<Long> descriptions) throws IOException {
		int count=0;
		boolean bPrim=true;
		bw.append("[");
		for (Long long1 : descriptions) {
			Rf2DescriptionRow rf2DescRow = rf2DescFile.getLastActiveRow(startDate, long1);
//			if (!newcomponents.contains(rf2DescRow.getConceptId())) {
				if (!bPrim){
					bw.append(",");
				}else{
					bPrim=false;
				}
				Description desc=new Description(long1.toString() , rf2DescRow.getEffectiveTime() , String.valueOf(rf2DescRow.getActive()) , rf2DescRow.getConceptId().toString() ,
						rf2DescRow.getLanguageCode() , rf2DescFile.getFsn(rf2DescRow.getTypeId()) , rf2DescRow.getTerm() ,
					    rf2DescFile.getFsn(rf2DescRow.getCaseSignificanceId()));
				bw.append(gson.toJson(desc).toString());
				desc=null;
				bw.append(sep);
				count++;
//			}
		}
		bw.append("]");
		bw.close();
		return count;
	}

	private ArrayList<Long> generatingRetiredConceptReasons(Rf2DescriptionFile rf2DescFile, Rf2ConceptFile conceptFile, Rf2AttributeValueRefsetFile attrValue, Rf2AssociationRefsetFile associationFile)
			throws FileNotFoundException, UnsupportedEncodingException, IOException {
		FileOutputStream fos;
		OutputStreamWriter osw;
		BufferedWriter bw;
		ArrayList<Long> retiredConcepts = conceptFile.getRetiredComponents(startDate, endDate);
		fos = new FileOutputStream(new File(outputDirectory, RETIRED_CONCEPT_REASON_FILE));
		logger.info("Generating " + RETIRED_CONCEPT_REASON_FILE);
		osw = new OutputStreamWriter(fos, "UTF-8");
		bw = new BufferedWriter(osw);
		int count=0;
		boolean bPrim=true;
		bw.append("[");
		for (Long long1 : retiredConcepts) {
			Rf2AttributeValueRefsetRow refsetRow = attrValue.getRowByReferencedComponentId(long1);
			Rf2AssociationRefsetRow associationRow = associationFile.getLastRowByReferencedComponentId(long1);

			String fsn = rf2DescFile.getFsn(long1);
			Pattern p = Pattern.compile("\\((.*?)\\)", Pattern.DOTALL);
			String semanticTag = "";
			if (fsn != null) {
				Matcher matcher = p.matcher(fsn);
				while (matcher.find()) {
					semanticTag = matcher.group(1);
				}
			}
			if (associationRow!=null) {
					String assValue = associationRow.getTargetComponent();
					if (!bPrim){
						bw.append(",");
					}else{
						bPrim=false;
					}
					if (refsetRow != null) {
						String value = refsetRow.getValueId();
						RetiredConcept concept=new RetiredConcept(long1.toString() ,rf2DescFile.getFsn(conceptFile.getDefinitionStatusId(long1)) , fsn , semanticTag,
								rf2DescFile.getFsn(Long.parseLong(value)),rf2DescFile.getFsn(Long.parseLong(associationRow.getRefsetId())),
								rf2DescFile.getFsn(Long.parseLong(assValue)) , String.valueOf(conceptFile.isNewComponent(long1, startDate)));
						bw.append(gson.toJson(concept).toString());
						
						concept=null;
					} else {
						RetiredConcept concept=new RetiredConcept(long1.toString() ,rf2DescFile.getFsn(conceptFile.getDefinitionStatusId(long1)) , fsn , semanticTag,
								"no reason",rf2DescFile.getFsn(Long.parseLong(associationRow.getRefsetId())),
								rf2DescFile.getFsn(Long.parseLong(assValue)) , String.valueOf(conceptFile.isNewComponent(long1, startDate)));
						bw.append(gson.toJson(concept).toString());
						concept=null;
					}
					bw.append(sep);
					count++;
			} else {
				if (!bPrim){
					bw.append(",");
				}else{
					bPrim=false;
				}
				if (refsetRow != null) {
					String value = refsetRow.getValueId();
					RetiredConcept concept=new RetiredConcept(long1.toString() ,rf2DescFile.getFsn(conceptFile.getDefinitionStatusId(long1)) , fsn , semanticTag,
							rf2DescFile.getFsn(Long.parseLong(value)),"no association","-" ,"-");
					bw.append(gson.toJson(concept).toString());
					concept=null;
				} else {
					RetiredConcept concept=new RetiredConcept(long1.toString() ,rf2DescFile.getFsn(conceptFile.getDefinitionStatusId(long1)) , fsn , semanticTag,
							"no reason","no association","-" ,"-");
					bw.append(gson.toJson(concept).toString());
					concept=null;
				}
				bw.append(sep);
				count++;
			}
		}
		bw.append("]");
		bw.close();
		attrValue.releasePreciousMemory();
		associationFile.releasePreciousMemory();

		addFileChangeReport(RETIRED_CONCEPT_REASON_FILE,count,"Inactivated concepts");
		
		return retiredConcepts;
		
	}

	private void addFileChangeReport(String fileName, int count, String reportName) {
		FileChangeReport fileChanges= new FileChangeReport();
		fileChanges.setFile(fileName);
		fileChanges.setCount(count);
		fileChanges.setName(reportName);
		List<FileChangeReport> lChanges= changeSummary.getReports();
		if (lChanges==null){
			lChanges=new ArrayList<FileChangeReport>();
		}
		lChanges.add(fileChanges);
		changeSummary.setReports(lChanges);
	}

	private void generateNewRelationshipsReport(Rf2DescriptionFile rf2DescFile, Rf2RelationshipFile relFile) throws FileNotFoundException, UnsupportedEncodingException, IOException {
		FileOutputStream fos;
		OutputStreamWriter osw;
		BufferedWriter bw;
		ArrayList<Long> newRels = relFile.getNewComponentIds(startDate);
		fos = new FileOutputStream(new File(outputDirectory, NEW_RELATIONSHIPS_FILE));
		logger.info("Generating new_relationships.json");
		osw = new OutputStreamWriter(fos, "UTF-8");
		bw = new BufferedWriter(osw);
		boolean bPrim=true;
		bw.append("[");
		for (Long long1 : newRels) {
			if (!bPrim){
				bw.append(",");
			}else{
				bPrim=false;
			}
			Rf2RelationshipRow row = relFile.getById(long1, startDate);
			Relationship rel=new Relationship(row.getId().toString() , String.valueOf(row.getActive()) , rf2DescFile.getFsn(row.getSourceId()),row.getSourceId().toString(), rf2DescFile.getFsn(row.getDestinationId()) ,
				 rf2DescFile.getFsn(row.getTypeId()) , rf2DescFile.getFsn(row.getCharacteristicTypeId()));
			bw.append(gson.toJson(rel).toString());
			rel=null;
			bw.append(sep);
		}
		bw.append("]");
		bw.close();

		addFileChangeReport(NEW_RELATIONSHIPS_FILE,newRels.size(),"New relationships");
	}

	private ArrayList<Long> generateNewConceptsReport(Rf2DescriptionFile rf2DescFile, Rf2ConceptFile conceptFile) throws FileNotFoundException, UnsupportedEncodingException, IOException {
		logger.info("getting new conscpt ids");
		ArrayList<Long> newcomponents = conceptFile.getNewComponentIds(startDate);
		FileOutputStream fos = new FileOutputStream(new File(outputDirectory, NEW_CONCEPTS_FILE));
		logger.info("Generating " + NEW_CONCEPTS_FILE);
		OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
		BufferedWriter bw = new BufferedWriter(osw);
		boolean bPrim=true;
		bw.append("[");
		for (Long long1 : newcomponents) {
			if (!bPrim){
				bw.append(",");
			}else{
				bPrim=false;
			}
			String fsn = rf2DescFile.getFsn(long1);
			Pattern p = Pattern.compile("\\((.*?)\\)", Pattern.DOTALL);
			String semanticTag = "";
			if (fsn != null) {
				Matcher matcher = p.matcher(fsn);
				while (matcher.find()) {
					semanticTag = matcher.group(1);
				}
			}
			Concept concept=new Concept(long1.toString() ,rf2DescFile.getFsn(conceptFile.getDefinitionStatusId(long1)) , fsn , semanticTag);
			bw.append(gson.toJson(concept).toString());
			concept=null;
			bw.append(sep);
		}
		bw.append("]");
		bw.close();

		addFileChangeReport(NEW_CONCEPTS_FILE,newcomponents.size(),"New concepts");
		
		return newcomponents;
	}


	public enum ReleaseFileType {
		DESCRIPTION, CONCEPT, RELATIONSHIP, ATTRIBUTE_VALUE_REFSET, ASSOCIATION_REFSET, LANGUAGE_REFSET
	}


	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}
}
