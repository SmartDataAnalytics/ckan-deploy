package org.aksw.commons.accessors;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

import org.aksw.commons.collections.SinglePrefetchIterator;

import com.google.common.base.Converter;

public class CollectionFromConverter<F, B>
	extends AbstractCollection<F>
{
	protected Collection<B> backend;
	protected Converter<F, B> converter;
	
	public CollectionFromConverter(Collection<B> backend, Converter<F, B> converter) {
//		Objects.requireNonNull(backend);
//		Objects.requireNonNull(converter);
		
		this.backend = backend;
		this.converter = converter;
	}

	@Override
	public boolean add(F value) {
		B item = converter.convert(value);
		boolean result = backend.add(item);
		
		return result;
	}
	
	@Override
	public boolean remove(Object o) {
		boolean result = false;
		try {
			B item = converter.convert((F)o);
			result = backend.remove(item);
		} catch(ClassCastException e) {
			
		}
		
		return result;
	}

	@Override
	public Iterator<F> iterator() {
		Iterator<B> baseIt = backend.iterator();

		return new SinglePrefetchIterator<F>() {
			@Override
			protected F prefetch() throws Exception {
				while(baseIt.hasNext()) {
					B b = baseIt.next();
					F f = converter.reverse().convert(b);
					return f;
				}
				return finish();
			}
			@Override
			public void doRemove() { baseIt.remove(); }
		};
	}
	
	@Override
	public int size() {
		return backend.size();
	}
}