package org.aksw.jena_sparql_api.pseudo_rdf;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.aksw.dcat.ap.binding.ckan.domain.impl.PropertySource;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.NodeVisitor;
import org.apache.jena.graph.Node_Concrete;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import org.apache.jena.util.iterator.WrappedIterator;

class PseudoNode
	extends Node_Concrete
{
	//protected PropertySource entity;
	//protected Map<String, Function<PropertySource, PseudoRdfProperty>> propertyToAccessor;

	protected Map<String, Function<PropertySource, PseudoRdfProperty>> propertyToAccessor;
	protected PropertySource source;
	
	//protected PropertySource source;
	//protected Function<? super PropertySource, ? extends Object> getIdentifier;
	
	
	// The property source acts as the label
	public PseudoNode(
			PropertySource source,
			Map<String, Function<PropertySource, PseudoRdfProperty>> propertyToAccessor) {
			//Function<? super PropertySource, ? extends Object> getIdentifier) {
		super(source);
		this.source = source;
		this.propertyToAccessor = propertyToAccessor;
	}
	
    public Map<String, Function<PropertySource, PseudoRdfProperty>> getPropertyToAccessor() {
		return propertyToAccessor;
	}

	public PropertySource getSource() {
		return source;
	}

//	public Object getIdentifier() {
//		Object result = getIdentifier.apply(source);
//		return result;
//	}



	@Override
    public boolean isBlank() { return true; }

    @Override
    public BlankNodeId getBlankNodeId()  { return (BlankNodeId) label; }
    
    @Override
    public Object visitWith(NodeVisitor v) {
    	throw new RuntimeException("Visiting not supported");
    }
    
    @Override
    public boolean equals( Object other )
    {
    	return false;
    }
    
    
    
	public PseudoRdfProperty getProperty(String property) {
		Function<PropertySource, PseudoRdfProperty> schemaProperty = propertyToAccessor.get(property);
		if(schemaProperty == null) {
			throw new RuntimeException("Property " + property + " not mapped for access over " + source);
		}
		
		PseudoRdfProperty result = schemaProperty.apply(source);
		return result;
	}

	public ExtendedIterator<Triple> listProperties(String p) {
		Set<String> pc = Collections.singleton(p);
		ExtendedIterator<Triple> result = listProperties(pc);
		return result;
	}
	
	public Collection<Node> getPropertyValues(String property) {
		PseudoRdfProperty p = getProperty(property);
	
		Collection<Node> result;
		try {
			result = p == null ? null : p.getValues();
		} catch(Exception e) {
			throw new RuntimeException("Error accessing property " + property, e);
		}
		return result;
	}

	
	public ExtendedIterator<Triple> listProperties() {
		Set<String> allKnownProperties = propertyToAccessor.keySet();
		return listProperties(allKnownProperties);
	}
    
	public ExtendedIterator<Triple> listProperties(Set<String> names) {

		ExtendedIterator<Triple> result = new NullIterator<>();
		for(String str : names) {
			Collection<Node> values = getPropertyValues(str);
			if(values != null) {
				Iterator<Node> tmp = values.iterator();
				ExtendedIterator<Node> it = WrappedIterator.create(tmp);
				
				//ExtendedIterator<? extends PseudoRdfNode> it = (ExtendedIterator<? extends PseudoRdfNode>)tmp;
	
				//Property p = ResourceFactory.createProperty(str);
				Node p = NodeFactory.createURI(str);
	
				result = result.andThen(it.mapWith(o -> new Triple(this, p, o)));//new StatementImpl(this, p, o)));
			}			
			//((ExtendedIterator<PseudoRdfNode>>)getPropertyValues(str).iterator())
		}
		
		return result;
	}

    
//        if ( this == other ) return true ;
//        return other instanceof Node_Blank && label.equals( ((Node_Blank) other).label ); }
}