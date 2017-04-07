package org.ihtsdo.changeanalyzer.file;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

public class Rf2ConceptFileTest {
	
	@Test
	public void testGetRetiredComponents() {
		String filePath = Rf2ConceptFileTest.class.getClassLoader().getResource("sct2_Concept_Full_Test_20170131.txt").getFile();
		String startDate ="20160801";
		String endDate ="20170131";
		Rf2ConceptFile conceptFile = new Rf2ConceptFile(filePath);
		ArrayList<Long> retired = conceptFile.getRetiredComponents(startDate, endDate);
		Assert.assertEquals(1, retired.size());
		Assert.assertEquals(1,conceptFile.getRetiredComponents(startDate,endDate).size());
	}

	
	@Test
	public void testGetNewlyInactive() {
		String filePath = Rf2ConceptFileTest.class.getClassLoader().getResource("sct2_Concept_Full_Test_NewlyInactive.txt").getFile();
		String startDate ="20160801";
		Rf2ConceptFile conceptFile = new Rf2ConceptFile(filePath);
		ArrayList<Long> bornInactive = conceptFile.getNewInactiveComponentIds(startDate);
		for(Long id : bornInactive) {
			System.out.println("Born inactive:" + id);
		}
		Assert.assertEquals(1,bornInactive.size());
		Assert.assertEquals("275780011",bornInactive.get(0).toString());
	}
	
	
	@Test
	public void testGetNewComponents() {
		String filePath = Rf2ConceptFileTest.class.getClassLoader().getResource("sct2_Concept_Full_Test_20170131.txt").getFile();
		//inclusive
		String startDate ="20160801";
		Rf2ConceptFile conceptFile = new Rf2ConceptFile(filePath);
		ArrayList<Long> newComponents = conceptFile.getNewComponentIds(startDate);
		for (Long sctId : newComponents) {
			System.out.println("concept:" + sctId);
		}
		Assert.assertEquals(3,newComponents.size());
	}
}
