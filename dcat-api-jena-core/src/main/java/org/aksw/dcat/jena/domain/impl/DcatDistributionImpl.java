package org.aksw.dcat.jena.domain.impl;

import java.util.Set;

import org.aksw.dcat.jena.domain.api.DcatDistribution;
import org.aksw.jena_sparql_api.utils.model.ResourceUtils;
import org.aksw.jena_sparql_api.utils.model.SetFromPropertyValues;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.DCTerms;

public class DcatDistributionImpl
	extends DcatEntityImpl
	implements DcatDistribution
{
	public DcatDistributionImpl(Node node, EnhGraph graph) {
		super(node, graph);
	}
	
	@Override
	public Set<Resource> getAccessURLs() {
		return new SetFromPropertyValues<>(this, DCAT.accessURL, Resource.class);
	}

	@Override
	public Set<Resource> getDownloadURLs() {
		return new SetFromPropertyValues<>(this, DCAT.downloadURL, Resource.class);
	}

	@Override
	public String getFormat() {
		return ResourceUtils.getLiteralPropertyValue(this, DCTerms.format, String.class).orElse(null);
	}

	@Override
	public void setFormat(String format) {
		ResourceUtils.setLiteralProperty(this, DCTerms.format, format);
	}
}
