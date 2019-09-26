package org.aksw.jena_sparql_api.data_client.algebra;

import org.aksw.jena_sparql_api.mapper.proxy.JenaPluginUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sys.JenaSystem;

public class MainTestDatasetTransformOps {
	public static void main(String[] args) {
		JenaSystem.init();
		
		JenaPluginUtils.scan(Op.class);
		
		Model m = ModelFactory.createDefaultModel();
		OpModel a = m.createResource().as(OpModel.class)
				.setDatasetId("myDatasetId");
		
		OpUpdateRequest b = m.createResource()
				.as(OpUpdateRequest.class)
				.addUpdateRequest("DELETE { ?s a ?o } WHERE { ?s a ?o }")
				.setSubOp(a);
		
		
		RDFDataMgr.write(System.out, m, RDFFormat.TURTLE_PRETTY);
	
		System.out.println(b.getSubOp() instanceof OpModel); 
		
		
		OpVisitor<Op> visitor = new OpMapper();
		
		Op result = b.accept(visitor);
		System.out.println("Result: " + result);
		
	}
}
