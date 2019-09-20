package org.aksw.dcat_suite.server.conneg;

import java.util.List;
import java.util.Set;

import org.aksw.jena_sparql_api.mapper.annotation.IriNs;
import org.aksw.jena_sparql_api.mapper.annotation.RdfType;
import org.aksw.jena_sparql_api.mapper.annotation.ResourceView;

@ResourceView
@RdfType("eg:EntityInfo")
public interface RdfEntityInfoDefault
	extends RdfEntityInfo
{
	@IriNs("eg")
	@Override
	List<String> getContentEncodings();
	
	@IriNs("eg")
	@Override
	String getContentType();
	

	@IriNs("eg")
	@Override
	Long getContentLength();

	/**
	 * Charset, such as UTF-8 or ISO 8859-1
	 * 
	 * @return
	 */
	@IriNs("eg")
	@Override
	String getCharset();

	/**
	 * The set of language tags for which the content is suitable.
	 * 
	 * @return
	 */
	@IriNs("eg")
	@Override
	Set<String> getLanguageTags();
	
	@IriNs("eg")
	@Override
	Set<HashInfo> getHashes();	
}
