package org.aksw.ckan_deploy.core;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import io.reactivex.Single;

/**
 * Interface for converting source files to target files.
 * The result type of Single should wrap the exit code of the operation.
 * Note, that {@link CompletableFuture} was not used because the standard implementation
 * does not support cancelling upstream - 
 * i.e. e.g. killing an underlying system process.
 * Also, exceptions are wrapped in the single which means that the methods do not need to declare them
 * which makes for a nicer interplay with lambdas.
 * 
 * @author raven
 *
 */
public interface PathCoder {
	Single<Integer> encode(Path input, Path output);
	Single<Integer> decode(Path input, Path output);
	boolean cmdExists();
}
