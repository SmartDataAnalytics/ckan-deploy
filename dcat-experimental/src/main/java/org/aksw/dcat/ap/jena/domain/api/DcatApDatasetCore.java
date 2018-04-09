package org.aksw.dcat.ap.jena.domain.api;

import java.time.Instant;
import java.util.Set;

public interface DcatApDatasetCore
	extends View
{
	/** Factory method for distributions - does not add them */
	DcatApDistribution createDistribution();
		

	String getTitle();
	void setTitle(String title);
	
	String getDescription();
	void setDescription(String description);
	
	Set<String> getTags();
	//void setTags(Set<String tags>);
	
	
	// A mix of URIs and strings
	Set<String> getThemes();
	
	String getIdentifier();
	void setIdentifier(String identifier);
	
	String getAlternateIdentifier();
	void setAlternateIdentifier(String alternateIdentifier);
	
	Instant getIssued();
	void setIssued(Instant issued);
	
	Instant getModified();
	void setModified(Instant modified);
	
	String getVersionInfo();
	void setVersionInfo(String versionInfo);
	
	String getVersionNotes();
	void setVersionNodes(String versionNotes);
	
	/** Language systems denoted by URIs */
	Set<String> getLanguages();
	void setLanguages(Set<String> languages);
	
	String getLandingPage();
	void setLandingPage(String landingPage);
	
	/** Frequency of updates denoted by an URI */
	String getAccrualPeriodicity();
	void setAccuralPeriodicity(String accrualPeriodicity);
	
	
	Set<String> getConformsTo();
	void setConformsTo(Set<String> conformsTo);

	String getAccessRights();
	void setAccessRights(String accessRights);
	
	Set<String> getPages();
	void setPage(Set<String> pages);
	
	String getProvenance();
	void setProvenance(String provenance);
	
	String getType();
	void setType(String type);
	
	/** Set of URIs to other datasets */
	Set<String> getHasVersions();
	void setHasVersions(Set<String> hasVersions);
	
	/** Set of URIs to other datasets */
	Set<String> getIsVersionOf();
	void setIsVersionOf(Set<String> isVersionOf);
	
	/** Set of URIs to other datasets */
	Set<String> getSources();
	void setSources(Set<String> sources);
	
	/** Set of URIs to distributions */
	Set<String> getSamples();
	void setSamples(Set<String> sources);
	
	Spatial getSpatial();
	void setSpatial(Spatial spatial);
	
	PeriodOfTime getTemporal();
	void setTemporal(PeriodOfTime temporal);
	
	DcatApAgent getPublisher();
	void setPublisher(DcatApAgent publisher);
	
	DcatApContactPoint getContactPoint();
	void setContactPoint(DcatApContactPoint contactPoint);
	
//	Set<? extends DcatApDistribution<M>> getDistributions();
//	void setDistributions(Set<? extends DcatApDistribution<M>> distributions);

	Set<DcatApDistribution> getDistributions();
	void setDistributions(Set<DcatApDistribution> distributions);
}
