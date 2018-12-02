package org.aksw.dcat.repo.api;

import org.aksw.dcat.jena.domain.api.DcatDataset;

import io.reactivex.Flowable;

public class DatasetResolverImpl
	implements DatasetResolver
{
	protected CatalogResolver catalogResolver;
	protected DcatDataset dcatDataset;

	public DatasetResolverImpl(CatalogResolver catalogResolver, DcatDataset dcatDataset) {
		this.catalogResolver = catalogResolver;
		this.dcatDataset = dcatDataset;
	}
	
	
	@Override
	public DcatDataset getDataset() {
		return dcatDataset;
	}

	@Override
	public Flowable<DistributionResolver> resolveDistribution(String distributionId) throws Exception {
		return catalogResolver.resolveDistribution(dcatDataset, distributionId);
	}



	@Override
	public CatalogResolver getCatalogResolver() {
		// TODO Auto-generated method stub
		return null;
	}



}