package org.aksw.jena_sparql_api.pseudo_rdf;

import org.aksw.jena_sparql_api.utils.model.NodeMapper;
import org.apache.jena.graph.Node;

import com.google.common.base.Converter;


public class ConverterFromNodeMapper<T>
	extends Converter<Node, T>
{
	protected NodeMapper<T> nodeMapper;
	
	public ConverterFromNodeMapper(NodeMapper<T> nodeMapper) {
		super();
		this.nodeMapper = nodeMapper;
	}

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
}
