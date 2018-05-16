package org.aksw.jena_sparql_api.pseudo_rdf;

import java.util.Collection;

import org.aksw.dcat.util.view.CollectionAccessor;
import org.aksw.dcat.util.view.CollectionFromConverter;
import org.aksw.jena_sparql_api.utils.model.NodeMapper;
import org.apache.jena.graph.Node;

import com.google.common.base.Converter;
import com.google.common.collect.Range;

public class PseudoRdfObjectPropertyImpl<T>
	implements PseudoRdfProperty
{
	/** The accessor to retrieve the enity */
	//protected SingleValuedAccessor<Collection<T>> accessor;
	protected CollectionAccessor<T> accessor;
	
	/** The mapper which converts between the underlying entity and nodes */
	protected NodeMapper<T> nodeMapper;

	protected RdfType<T> rdfType;

	/** The supplier of new entities */ 
	//protected RdfType rdfType;
	
	// The available schematic properties 
	//protected Map<String, Function<T, PseudoRdfProperty>> propertyToAccessor;
	
	
	public PseudoRdfObjectPropertyImpl(
			//SingleValuedAccessor<Collection<T>> accessor,
			CollectionAccessor<T> accessor,
			RdfType<T> rdfType,
			NodeMapper<T> nodeMapper) {
			//Function<T, Node> backendToNode) {
		super();
		this.accessor = accessor;
		//this.backendToNode = backendToNode;
		this.rdfType = rdfType;
		this.nodeMapper = nodeMapper;
	}
	
	@Override
	public Collection<Node> getValues() {
		//SetFromSingleValuedAccessor<T> backend = new SetFromSingleValuedAccessor<>(accessor);
		
		Converter<Node, T> converter = new Converter<Node, T>() {
			@Override
			protected T doForward(Node a) {
				T result = nodeMapper.toJava(a);
				return result;
			}

			@Override
			protected Node doBackward(T b) {
				Node result = nodeMapper.toNode(b);
				return result;
			}
		};
		
		
		Collection<T> backend = accessor.get();
		
		if(backend == null) {
			throw new RuntimeException("Got null value for " + accessor);
		}
		
		Collection<Node> result = new CollectionFromConverter<>(backend, converter);
		return result;
	}

	@Override
	public Range<Long> getMultiplicity() {
		Range<Long> result = accessor.getMultiplicity();
		return result;
	}
	
	@Override
	public NodeMapper<T> getNodeMapper() {
		return nodeMapper;
	}
	
	@Override
	public RdfType<T> getType() {
		return rdfType;
	}
}

