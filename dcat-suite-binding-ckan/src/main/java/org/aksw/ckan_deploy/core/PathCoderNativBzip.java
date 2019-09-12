package org.aksw.ckan_deploy.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.aksw.commons.util.compress.MetaBZip2CompressorInputStream;

import io.reactivex.Single;

public class PathCoderNativBzip
	implements PathCoder
{
	@Override
	public boolean cmdExists() {
		return true;
	}
	
	@Override
	public Single<Integer> decode(Path input, Path output) {
		try(InputStream in = new MetaBZip2CompressorInputStream(Files.newInputStream(input))) {
			Files.copy(in, output);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return Single.just(0);
	}

	@Override
	public Single<Integer> encode(Path input, Path output) {
		throw new RuntimeException("not implemented yet");
//		try(InputStream in = new BZip2(Files.newInputStream(input))) {
//			Files.copy(in, output);
//		}
		//return Single.just(0);
	}
}
