package org.aksw.jena_sparql_api.conjure.graph.fs;

import static org.apache.jena.query.ReadWrite.WRITE;
import static org.apache.jena.system.Txn.calculateRead;
import static org.apache.jena.system.Txn.executeWrite;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.TxnType;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.JenaTransactionException;
import org.apache.jena.sparql.core.DatasetChanges;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.DatasetGraphWrapper;
import org.apache.jena.sparql.core.GraphView;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.QuadAction;
import org.apache.jena.sparql.core.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A DatasetGraph that is backed by a file.
 * The file is always written out whenever a write transaction is committed.
 * On first access the file content cached is cached memory.
 * TODO Implement: On the start of each transaction it is checked whether the file was changed and it is reread as necessary.
 *
 * Acquiring a read or write lock will lock the file for other processes.
 * Hence, another process cannot write the file while a read transaction is running.
 *
 */
public class DatasetGraphWithSync
     extends DatasetGraphWrapper
//    extends DatasetGraphMonitor
{
    private static final Logger logger = LoggerFactory.getLogger(DatasetGraphWithSync.class);

    protected Transactional syncher;

    protected AtomicLong generation = new AtomicLong(1l);

    /** Upon starting a transaction the version is set to the generation's value
     *  Upon start of a transaction the generation is stored as the version.
     *  Each mutation (change of state) increases the version.
     *  Upon commit the generation is set to the version of the committing transaction
     */
    protected ThreadLocal<Long> version = ThreadLocal.withInitial(() -> null);

    public static class GraphViewSimple
        extends GraphView
    {
        public GraphViewSimple(DatasetGraph dsg, Node gn) {
            super(dsg, gn);
        }

//        @Override
//        protected PrefixMapping createPrefixMapping() {
//            final DatasetPrefixStorage prefixes = datasetGraph().prefixes();
//            return isDefaultGraph() || isUnionGraph() ? prefixes.getPrefixMapping() : prefixes
//                .getPrefixMapping(getGraphName().getURI());
//        }
    }

    /** A cache to ensure that the same graph view is returned for each name. */
    protected Map<Node, Graph> graphViewCache = Collections.synchronizedMap(new HashMap<>());

    @Override
    public Graph getGraph(Node graphNode) {
        // return super.getGraph(graphNode);
//    	Graph tmp = new GraphViewSimple(this, graphNode);
        Graph result = graphViewCache.computeIfAbsent(graphNode, n -> new GraphViewSimple(this, n));
        return result;
    }

    public DatasetGraphWithSync(Path path, LockPolicy lockPolicy) throws Exception {
        this(DatasetGraphFactory.createTxnMem(), path, lockPolicy);
    }

    public DatasetGraphWithSync(DatasetGraph dsg, Path path, LockPolicy lockPolicy) throws Exception {
        super(dsg);
        RDFFormat rdfFormat = RDFFormat.TRIG_PRETTY;
        syncher = new FileSyncGraph(dsg, path, rdfFormat, lockPolicy, this::getVersion);
    }


    @Override
    public boolean supportsTransactions() {
        return true;
    }

    @Override
    public void begin() {
        begin(TxnType.READ);
    }

    @Override
    public void begin(TxnType type) {
        ReadWrite readWrite = TxnType.READ_PROMOTE.equals(type) ? ReadWrite.WRITE : TxnType.convert(type);
        begin(readWrite);
    }

    protected Long getVersion() {
        Long result = generation.get();
//        System.out.println("VERSION = " + result);
        return result;
    }

    /**
     * Beginning a transaction always starts with acquisition
     * of a lock on the file opened either in read or write mode.
     *
     */
    @Override
    public void begin(ReadWrite readWrite) {
        // FIXME - The statement below is wrong: If a read transaction is requested but the data
        // has not been loaded that we need to lock the in memory model with write for the load phase

        // Prepare the txn on the in memory model first, because we may need to
        // load the data from the file

        version.set(generation.get());
        syncher.begin(readWrite);
        super.begin(readWrite);

//        try {
//            super.begin(readWrite);
//        } catch(Exception e) {
//            syncher.end();//finishTransaction();
//            throw new RuntimeException(e);
//        }
    }

    @Override
    public void commit() {
        try {
            // FIXME the second commit may fail even after writing to file succeeded
            // Then we'd have a desync

            long gen = generation.get();

            // Make the version negative to mark it as 'dirty'
            long v = version.get();
            boolean isDirty = v < 0;
            v = Math.abs(v);

            if (isDirty) { // || transactionMode().equals(WRITE)) {
                if (v != gen) {
                    throw new InternalErrorException(String.format("Version=%d, Generation=%d", v, gen)) ;
                }
                generation.incrementAndGet() ;
            }

            syncher.commit();
            super.commit();

        } finally {
            syncher.end();
        }
    }

    @Override
    public void abort() {
        super.abort();
    }

    @Override
    public void close() {
        if (isInTransaction()) {
            abort();
        }

        if(syncher instanceof AutoCloseable) {
            try {
                ((AutoCloseable)syncher).close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        super.close();
    }

    @Override
    public void end() {
        super.end();
        syncher.end();
        version.remove();
    }

    /**
     * Copied from {@link DatasetGraphWrapper}
     *
     * @param <T>
     * @param mutator
     * @param payload
     */
    private <T> void mutate(final Consumer<T> mutator, final T payload) {
        if (isInTransaction()) {
            if (!transactionMode().equals(WRITE)) {
                TxnType mode = transactionType();
                switch (mode) {
                case WRITE:
                    break;
                case READ:
                    throw new JenaTransactionException("Tried to write inside a READ transaction!");
                case READ_COMMITTED_PROMOTE:
                case READ_PROMOTE:
                    throw new RuntimeException("promotion not implemented");
//                    boolean readCommitted = (mode == TxnType.READ_COMMITTED_PROMOTE);
//                    promote(readCommitted);
                    //break;
                }
            }

            // Make the version negative to mark it as 'dirty'
            version.set(-Math.abs(version.get()));

            mutator.accept(payload);
        } else executeWrite(this, () -> {
            version.set(-Math.abs(version.get()));
//            System.out.println(version.get());
            mutator.accept(payload);
        });
    }

    @Override
    public void clear() {
        mutate(x -> {
            getW().clear();
        } , null);
    }


    @Override
    public void add(Quad quad)
    { mutate(x -> getW().add(quad), null); }

    @Override
    public void delete(Quad quad)
    { mutate(x -> getW().delete(quad), null); }

    @Override
    public void add(Node g, Node s, Node p, Node o)
    { mutate(x -> getW().add(g, s, p, o), null); }

    @Override
    public void delete(Node g, Node s, Node p, Node o)
    { mutate(x -> getW().delete(g, s, p, o), null); }

    @Override
    public void deleteAny(Node g, Node s, Node p, Node o)
    { mutate(x -> getW().deleteAny(g, s, p, o), null); }

    private <T> T access(final Supplier<T> source) {
        return isInTransaction() ? source.get() : calculateRead(this, source::get);
    }

    @Override
    public Iterator<Node> listGraphNodes() {
        return access(() -> getR().listGraphNodes());
    }



    public static void main(String[] args) throws Exception {
        Path file = Paths.get("/tmp/txn-test.trig");

        DatasetGraph dg = new DatasetGraphWithSync(file, LockPolicy.LIFETIME);
        Dataset ds = DatasetFactory.wrap(dg);

        dg.clear();

        List<String> test = IntStream.range(0, 1000)
                //.mapToObj(i -> new Triple(RDF.type.asNode(), RDF.type.asNode(), NodeFactory.createLiteral("" + i)))
                .mapToObj(i -> "INSERT DATA { <foo> <bar> " + i + "}")
                .collect(Collectors.toList());

        test.parallelStream().forEach(stmt -> {
                    System.out.println(Thread.currentThread() + " working");
                    RDFConnection conn = RDFConnectionFactory.connect(ds);
                    conn.begin(ReadWrite.WRITE);
                    conn.update(stmt);
                    conn.commit();
        });
    }



    public static DatasetChanges createMonitor() {
        DatasetChanges result = new DatasetChanges() {
            @Override
            public void start() {
                System.out.println("start");
            }

            @Override
            public void reset() {
                System.out.println("reset");
            }

            @Override
            public void finish() {
                System.out.println("finish");
            }

            @Override
            public void change(QuadAction qaction, Node g, Node s, Node p, Node o) {
                System.out.println(Arrays.asList(qaction, g, s, p, o).stream()
                        .map(Objects::toString).collect(Collectors.joining(", ")));
            }
        };

        return result;
    }

}


//
//if (false) {
//  syncher = new FileSyncBase(lockPolicy, path) {
////            @Override
////            public void begin(ReadWrite readWrite) {
////                super.begin(readWrite);
////
////            }
//      @Override
//      protected void loadFrom(FileChannel localFc) {
//          logger.info("Loading data from " + path);
////                if(!dsg.isInTransaction()) {
////                    throw new RuntimeException("we should be in a transaction here");
////                }
//          dsg.clear();
//
//          // The input stream is intentionally not closed;
//          // as it would close the file cannel.
//          // The locks depend on the file channel, so the channel
//          // needs to remain open for the time of transaction
//          Lang lang = rdfFormat.getLang();
//          InputStream in = new CloseShieldInputStream(Channels.newInputStream(localFc));
//          RDFDataMgr.read(getW(), in, lang);
//      }
//
//      @Override
//      protected void storeTo(FileChannel localFc) {
//          OutputStream out = new CloseShieldOutputStream(Channels.newOutputStream(localFc));
//          RDFDataMgr.write(out, dsg, rdfFormat);
//      }
//  };
//}

//this.syncher = syncher;
