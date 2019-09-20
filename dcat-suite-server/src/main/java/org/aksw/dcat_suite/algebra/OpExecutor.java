package org.aksw.dcat_suite.algebra;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Objects;

import org.aksw.ckan_deploy.core.PathCoder;
import org.aksw.ckan_deploy.core.PathCoderRegistry;
import org.aksw.dcat_suite.server.conneg.HashSpace;
import org.aksw.dcat_suite.server.conneg.HashSpaceImpl;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;

import io.reactivex.Single;

public class OpExecutor
	implements OpVisitor<Path>
{
	protected HashSpace hashSpace;
	protected OpVisitor<String> hasher;

	public OpExecutor() {
		this.hasher = new Hasher(Collections.emptyMap());
		hashSpace = new HashSpaceImpl(Paths.get("/home/raven/.dcat/test3/hashspace"));
	}
//	
//	public Path cacheLookup(Op op, Function<Op, > ) {
//		String hash = op.accept(hasher);
//		Path path = hashSpace.get(hash);
//		
//		if(path != null) {
//			
//		}
//		
//	}
	
	@Override
	public Path visit(OpCode op) {
		String coderName = op.getCoderName();
		PathCoder coder = PathCoderRegistry.get().getCoder(coderName);

		Objects.requireNonNull(coder, "No coder for " + coderName);		
		Path srcPath = op.accept(this);
		
		Path tgtPath = getTargetPath(op);
		
		coder.encode(srcPath, tgtPath)
			.blockingGet();
		
		return tgtPath;
	}
	
	public Path getTargetPath(Op op) {
		String hash = op.accept(hasher);
		Path result = hashSpace.get(hash);
		
		return result;
	}

	@Override
	public Path visit(OpConvert op) {
		
		Path srcPath = op.accept(this);
		Path tgtPath = getTargetPath(op);
		
		String srcContentType = op.getSourceContentType();
		String tgtContentType = op.getTargetContentType();
		
		convert(srcPath, tgtPath, srcContentType, tgtContentType);
		
		return tgtPath;
	}

	@Override
	public Path visit(OpVar op) {
		String hash = op.getName();
		//hashSpace.get(hash)
		
		return null;
	}
	
	@Override
	public Path visit(OpValue op) {
		String str = Objects.toString(op.getValue());
		Path result = Paths.get(str);
		return result;
	}

	
	public static Single<Integer> convert(Path srcPath, Path tgtPath, String srcContentType, String tgtContentType) {
		
		Model m = ModelFactory.createDefaultModel();
		
		
		Lang srcLang = RDFLanguages.nameToLang(srcContentType);
		Lang tgtLang = RDFLanguages.nameToLang(tgtContentType);

		try {
			RDFDataMgr.read(m, Files.newInputStream(srcPath, StandardOpenOption.READ), srcLang);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}

		try(OutputStream out = Files.newOutputStream(tgtPath,
				StandardOpenOption.CREATE,
				StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING)) {
			RDFDataMgr.write(out, m, tgtLang);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return Single.just(0);

	}
	
//	public CompletableFuture<FileEntityEx> execPlan(FileEntityEx source, List<TransformStep> plan) throws Exception {
//
//		//List<TransformStep> plan = createPlan(source, rawTgtBasePath, tgtContentType, tgtEncodings);		
//		Path src = source.getRelativePath();
//		Path dest = src;
//
//		// Execute the plan
//		for(TransformStep step : plan) {
//			dest = step.destPath;
//			
//			if(!Files.exists(dest)) {
//
//				Path tmp = allocateTmpFile(dest);
//				logger.info("Creating " + dest + " from " + src + " via tmp " + tmp);
//				step.method.apply(src, tmp)
//					.blockingGet();
//				
//				Files.move(tmp, dest, StandardCopyOption.ATOMIC_MOVE);
//			}
//			
//
//			src = dest;
//		}
//		
//		System.out.println("Generated: " + dest);
//
//		return null;
//	}

}