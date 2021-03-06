package org.aksw.ckan_deploy.core;

import java.util.LinkedHashMap;
import java.util.Map;

import org.aksw.jena_sparql_api.conjure.entity.utils.PathCoder;
import org.aksw.jena_sparql_api.conjure.entity.utils.PathCoderLbZip;
import org.aksw.jena_sparql_api.conjure.entity.utils.PathCoderNativBzip;

public class PackerRegistry {
	protected Map<String, PathCoder> mimeTypeToTransform;
	
	public PackerRegistry(Map<String, PathCoder> mimeTypeToTransform) {
		this.mimeTypeToTransform = mimeTypeToTransform;
	}
	
	public Map<String, PathCoder> getMap() {
		return mimeTypeToTransform;
	}
	
	public static PackerRegistry createDefault() {
		Map<String, PathCoder> mimeTypeToTransform = new LinkedHashMap<>();

		String mimeType = "application/x-bzip";
		
		PathCoder lbzip = new PathCoderLbZip();
		if(lbzip.cmdExists()) {
			mimeTypeToTransform.put(mimeType, lbzip);
		} else {
			lbzip = new PathCoderNativBzip();
			mimeTypeToTransform.put(mimeType, lbzip);
		}
		
		return new PackerRegistry(mimeTypeToTransform);
	}
}
