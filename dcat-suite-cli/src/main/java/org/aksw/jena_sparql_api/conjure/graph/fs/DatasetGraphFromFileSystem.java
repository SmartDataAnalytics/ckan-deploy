package org.aksw.jena_sparql_api.conjure.graph.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.aksw.jena_sparql_api.http.repository.impl.UriToPathUtils;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.ext.com.google.common.collect.Streams;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.TxnType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.SystemARQ;
import org.apache.jena.sparql.core.DatasetChanges;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphCollection;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.DatasetGraphFactory.GraphMaker;
import org.apache.jena.sparql.core.DatasetGraphMonitor;
import org.apache.jena.sparql.core.QuadAction;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.graph.GraphReadOnly;
import org.apache.jena.system.Txn;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;


public class DatasetGraphFromFileSystem
    extends DatasetGraphCollection
{
    protected Path basePath;
    protected PathMatcher pathMatcher;
    protected Predicate<? super Path> isPathException;

    protected TreeMap<Path, Dataset> individualCache = new TreeMap<Path, Dataset>();

    // Relative path to basePath
    protected TreeMap<Path, Dataset> relPathToDataset = null;

    protected Graph dftGraph = new GraphReadOnly(GraphFactory.createDefaultGraph());


    protected Transactional txn;
    protected TxnDataset2Graph2 txnDsg2Graph;


    public DatasetGraphFromFileSystem(
            Path basePath,
            PathMatcher pathMatcher,
            Predicate<? super Path> isPathException) {
        super();
        this.basePath = basePath;
        this.pathMatcher = pathMatcher;
        this.isPathException = isPathException;

        txnDsg2Graph = new TxnDataset2Graph2(dftGraph);
        txn = txnDsg2Graph;
    }

    @Override
    public void commit() {
        if ( txnDsg2Graph == null )
            SystemARQ.sync(this);
        txn.commit() ;
    }

    @Override public void begin()                       { txn.begin(); }
    @Override public void begin(TxnType txnType)        { txn.begin(txnType); }
    @Override public void begin(ReadWrite mode)         { txn.begin(mode); }
    @Override public boolean promote(Promote txnType)   { return txn.promote(txnType); }
    //Above: commit()
    @Override public void abort()                       { txn.abort(); }
    @Override public boolean isInTransaction()          { return txn.isInTransaction(); }
    @Override public void end()                         { txn.end(); }
    @Override public ReadWrite transactionMode()        { return txn.transactionMode(); }
    @Override public TxnType transactionType()          { return txn.transactionType(); }
    @Override public boolean supportsTransactions()     { return true; }
    @Override public boolean supportsTransactionAbort() { return false; }


    public static DatasetGraphFromFileSystem createDefault(Path basePath) {
        PathMatcher pathMatcher = basePath.getFileSystem().getPathMatcher("glob:**/*.trig");
        DatasetGraphFromFileSystem result = new DatasetGraphFromFileSystem(
                basePath,
                pathMatcher,
                path -> false);

        return result;
    }

    public static Stream<Path> listPaths(
            Path startPath,
            PathMatcher pathMatcher,
            Predicate<? super Path> isIgnored) throws IOException {
        return null;
//    	Files.walk
//        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
//            @Override
//            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                if(pathMatcher.matches(file)) {
//                	boolean isExcluded = !isIgnored.test(file);
//
//                    if(!isExcluded) {
//                    }
//                }
//                return FileVisitResult.CONTINUE;
//            }
//        });
    }

    public void rescan() throws IOException {
        relPathToDataset = Files.walk(basePath)
            .filter(pathMatcher::matches)
            .filter(path -> !isPathException.test(path))
            .collect(Collectors.toMap(
                    path -> basePath.relativize(path),
                    path -> {
                        Path parentPath = basePath.relativize(path.getParent());
                        Entry<Path, Dataset> e = getOrCreate(parentPath);
                        return e.getValue();
//                        Path fullPath = path.toAbsolutePath();
//                        DatasetGraph dsg;
//                        try {
//                            dsg = new DatasetGraphWithSync(DatasetGraphFactory.createTxnMem(), LockPolicy.TRANSACTION, fullPath);
//                        } catch (Exception e) {
//                            throw new RuntimeException(e);
//                        }
//                        Dataset ds = DatasetFactory.wrap(dsg);
//                        return ds;
                    },
                    (u, v) -> { throw new RuntimeException("Duplicate key: " + u + " - " + v); },
                    TreeMap::new));

    }

    public void loadAllGraphs() {
        try {
            if (individualCache != null) {
                relPathToDataset = individualCache;
                individualCache = null;
            }
            rescan();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterator<Node> listGraphNodes() {
        loadAllGraphs();

        Iterator<Node> result = relPathToDataset.values().stream()
            .flatMap(ds -> {
                Iterator<Node> it = ds.asDatasetGraph().listGraphNodes();
                Stream<Node> r = Streams.stream(it);
                return r;
            })
            .distinct()
            .iterator();

        return result;
    }

    public static DatasetGraph create(Path basePath) {
        GraphMaker graphMaker = new GraphMakerFromFileSystem(basePath);
        DatasetGraphMapLink2 result = new DatasetGraphMapLink2(
                GraphFactory.createJenaDefaultGraph(),
                graphMaker);

        return result;
    }


    @Override
    public Graph getDefaultGraph() {
        // Maybe return an immutable empty graph?
        return dftGraph;
    }

    @Override
    public Graph getGraph(Node graphNode) {
        Entry<Path, Dataset> e = getOrCreateGraph(graphNode);
        Graph result = e.getValue().asDatasetGraph().getGraph(graphNode);
        return result;
    }

    protected Map<Path, Dataset> getTargetMap() {
        Map<Path, Dataset> result = relPathToDataset != null
                ? relPathToDataset
                : individualCache;
        return result;
    }

    @Override
    public void addGraph(Node graphName, Graph graph) {
        Graph tgt = getGraph(graphName);
        tgt.clear();
        GraphUtil.addInto(tgt, graph);
    }


    public Entry<Path, Dataset> getOrCreateGraph(Node graphName) {
        // Graph named must be a URI
        String iri = graphName.getURI();
        Path relPath = UriToPathUtils.resolvePath(iri);
        Entry<Path, Dataset> result = getOrCreate(relPath);

        if (txnDsg2Graph != null) {
            // Ensure that the requested graphName is added to the txn handlers
            DatasetGraph dsg = result.getValue().asDatasetGraph();
            txnDsg2Graph.addGraph(dsg.getGraph(graphName));
        }

        return result;
    }

    public Entry<Path, Dataset> getOrCreate(Path relPath) {
        Map<Path, Dataset> targetMap = getTargetMap();

        Path fileRelPath = relPath.resolve("data.trig");
        Dataset ds = targetMap.get(fileRelPath);
        if (ds == null) {
            Path fullPath = basePath.resolve(fileRelPath);//.resolve("data.trig");

            DatasetGraph dsg;
            try {
                dsg = new DatasetGraphWithSync(fullPath, LockPolicy.TRANSACTION);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            ds = DatasetFactory.wrap(dsg);
            targetMap.put(fileRelPath, ds);

            if (txnDsg2Graph != null) {
                List<Node> graphNodes = Streams.stream(dsg.listGraphNodes()).collect(Collectors.toList());
                for (Node graphNode : graphNodes) {
                    txnDsg2Graph.addGraph(dsg.getGraph(graphNode));
                }
            }

        }

        Entry<Path, Dataset> result = Maps.immutableEntry(fileRelPath, ds);

        return result;
    }

    @Override
    public void removeGraph(Node graphName) {
        String iri = graphName.getURI();
        Path relPath = UriToPathUtils.resolvePath(iri);

        Path fileRelPath = relPath.resolve("data.trig");
        Map<Path, Dataset> targetMap = getTargetMap();
        Dataset ds = targetMap.get(fileRelPath);

        // TODO Finish

        if (txnDsg2Graph != null) {
            // Ensure that the requested graphName is added to the txn handlers
//            DatasetGraph dsg = result.getValue().asDatasetGraph();
//            txnDsg2Graph.addGraph(dsg.getGraph(graphName));
        }


    }


    public static void main(String[] args) throws IOException {
        Path path = Paths.get("/tmp/graphtest");
        Files.createDirectories(path);
        DatasetGraph raw = DatasetGraphFromFileSystem.createDefault(path);

        DatasetGraph dg = raw;

//        DatasetGraph dg = new DatasetGraphMonitor(raw, new DatasetChanges() {
//            @Override
//            public void start() {
//                System.out.println("start");
//            }
//
//            @Override
//            public void reset() {
//                System.out.println("reset");
//            }
//
//            @Override
//            public void finish() {
//                System.out.println("finish");
//            }
//
//            @Override
//            public void change(QuadAction qaction, Node g, Node s, Node p, Node o) {
//                System.out.println(Arrays.asList(qaction, g, s, p, o).stream()
//                        .map(Objects::toString).collect(Collectors.joining(", ")));
//            }
//        }, true);

        if (true) {
            System.out.println("graphnodes:" + Streams.stream(dg.listGraphNodes()).collect(Collectors.toList()));
//            RDFDataMgr.write(System.out, dg, RDFFormat.TRIG_BLOCKS);

    //        Txn.executeWrite(dg, () -> {
                dg.add(RDF.Nodes.type, RDF.Nodes.type, RDF.Nodes.type, RDF.Nodes.type);
    //        });

            System.out.println("Adding another graph");
    //        Txn.executeWrite(dg, () -> {
                dg.add(RDFS.Nodes.label, RDFS.Nodes.label, RDFS.Nodes.label, RDFS.Nodes.label);
    //        });
        }

        Model m = RDFDataMgr.loadModel("dcat-ap-ckan-mapping.ttl");
        Txn.executeWrite(dg, () -> {
            DatasetFactory.wrap(dg).addNamedModel(OWL.Class.getURI(), m);
        });

        DatasetFactory.wrap(dg).getNamedModel(OWL.Class.getURI()).add(RDF.Bag, RDF.type, RDF.Bag);

        System.out.println("done");
    }

}
