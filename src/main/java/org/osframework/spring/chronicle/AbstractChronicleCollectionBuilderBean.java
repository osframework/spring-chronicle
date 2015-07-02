package org.osframework.spring.chronicle;

import net.openhft.chronicle.hash.ChronicleHashBuilder;
import net.openhft.chronicle.hash.ChronicleHashErrorListener;
import net.openhft.lang.io.serialization.BytesMarshaller;
import net.openhft.lang.io.serialization.ObjectSerializer;
import net.openhft.lang.model.Byteable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Abstract superclass of <a href="http://chronicle.software/">Chronicle</a> collection
 * {@linkplain org.springframework.beans.factory.FactoryBean} classes.
 *
 * @param <K> Key type of Chronicle collection to be built
 * @param <T> Type of Chronicle collection to be built
 *
 * @author <a href="mailto:dave@osframework.org">Dave Joyce</a>
 * @since 0.0.1
 */
public abstract class AbstractChronicleCollectionBuilderBean<K, T> extends AbstractFactoryBean<T> {

    /**
     * Default maximum number of entries stored by collection instance created
     * by this object.
     */
    protected static final long DEFAULT_ENTRIES = 1 << 20;

    protected final Logger slf4jLogger;

    /**
     * Default constructor. Initializes logger for use by subclasses.
     */
    public AbstractChronicleCollectionBuilderBean() {
        super();
        this.slf4jLogger = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Set key type of the collection instance this object builds.
     *
     * @param keyClass class of the set value type
     */
    public void setKeyClass(Class<K> keyClass) {
        getConfig().keyClass = keyClass;
    }

    /**
     * Set the {@code BytesMarshaller} used to serialize/deserialize keys to/from off-heap
     * memory in the collection instance created by this object. See
     * <a href="https://github.com/OpenHFT/Chronicle-Map#serialization">the section about serialization in ChronicleMap
     * manual</a> for more information.
     *
     * @param keyMarshaller marshaller used to serialize keys
     */
    public void setKeyMarshaller(BytesMarshaller<? super K> keyMarshaller) {
        getConfig().keyMarshaller = keyMarshaller;
    }

    /**
     * Set the average number of bytes, taken by serialized form of keys, put into the
     * collection instance this object builds. If key size is always the same, call
     * {@link #setConstantKeySizeBySample(Object)} method instead of this one.
     * <p>If key is a boxed primitive type or {@code Byteable} subclass, i. e. if key size is known
     * statically, it is automatically accounted and shouldn't be specified by user.</p>
     *
     * @param averageKeySize average number of bytes, taken by serialized form of keys
     * @throws IllegalArgumentException if argument is not positive number
     * @throws IllegalStateException if {@link #setConstantKeySizeBySample(Object) sample key} has been specified
     */
    public void setAverageKeySize(double averageKeySize) {
        getConfig().averageKeySize = averageKeySize;
        getConfig().checkKeySizing();
    }

    /**
     * Set the constant number of bytes, taken by serialized form of keys, put into the
     * collection instance this object builds. If key size varies, call
     * {@link #setAverageKeySize(double)} method instead of this one. By providing the
     * {@code sampleKey}, all keys should take the same number of bytes in serialized form,
     * as this sample object.
     * <p>If key is a boxed primitive type or {@code Byteable} subclass, i. e. if key size is known
     * statically, it is automatically accounted and shouldn't be specified by user.</p>
     *
     * @param sampleKey sample key with constant number of bytes for all keys
     * @throws IllegalStateException if {@link #setAverageKeySize(double) avg key size} has been specified
     */
    public void setConstantKeySizeBySample(K sampleKey) {
        getConfig().sampleKey = sampleKey;
        getConfig().checkKeySizing();
    }

    /**
     * Set maximum number of entries contained by the collection instances created by this object.
     *
     * @param maxEntries maximum number of map entries
     * @see net.openhft.chronicle.hash.ChronicleHashBuilder#entries(long)
     */
    public final void setMaxEntries(long maxEntries) {
        getConfig().maxEntries = maxEntries;
    }

    /**
     * Set actual maximum number of entries that could be inserted into any single segment
     * of collection instances created by this object. Configuring both the actual number of
     * entries per segment and {@link #setActualSegments(int) actual segments} replaces a
     * single {@link #setMaxEntries(long)} configuration.
     * <p>This is a <em>low-level configuration</em>.</p>
     *
     * @param entriesPerSegment actual maximum number of entries per segment in maps constructed by this object
     * @see #setMaxEntries(long)
     * @see #setActualSegments(int)
     */
    public final void setEntriesPerSegment(long entriesPerSegment) {
        getConfig().entriesPerSegment = entriesPerSegment;
    }

    /**
     * Set the actual number of chunks that will be reserved for any single segment of the
     * collection instances created by this object. This configuration is a lower-level version of
     * {@link #setEntriesPerSegment(long)}.
     * <p>Setting this property makes sense only if {@link #setActualChunkSize(int)},
     * {@link #setActualSegments(int)}, and {@link #setEntriesPerSegment(long)} are also configured
     * manually.</p>
     *
     * @param actualChunksPerSegment the actual number of chunks reserved per segment in the
     *                               collection instances created by this object
     */
    public final void setActualChunksPerSegment(long actualChunksPerSegment) {
        getConfig().actualChunksPerSegment = actualChunksPerSegment;
    }

    /**
     * Set the actual number of segments in collection instances created by this object.
     * With {@link #setEntriesPerSegment(long) actual number of segments}, this configuration
     * replaces a single {@link #setMaxEntries(long)} call.
     * <p>This is a <em>low-level configuration</em>.</p>
     *
     * @param actualSegments actual number of segments in maps constructed by this object
     * @see #setEntriesPerSegment(long)
     */
    public final void setActualSegments(int actualSegments) {
        getConfig().actualSegments = actualSegments;
    }

    /**
     * Set minimum number of segments in collection instances created by this object.
     *
     * @param minSegments minimum number of segments in maps constructed by this object
     */
    public final void setMinSegments(int minSegments) {
        getConfig().minSegments = minSegments;
    }

    /**
     * Set the size in bytes of the allocation unit of collection instances this object builds.
     * <p>To minimize memory overuse and improve speed, one should pay decent attention to this
     * configuration. Alternatively, just trust the heuristics and don't configure
     * the chunk size.</p>
     * <p>Specify chunk size so that most entries would take from 5 to several dozens of chunks.
     * However, remember that operations with entries that span several chunks are a bit slower,
     * than with entries which take a single chunk. Particularly avoid entries to take more than
     * 64 chunks.</p>
     *
     * @param actualChunkSize the "chunk size" in bytes
     * @see net.openhft.chronicle.hash.ChronicleHashBuilder#actualChunkSize(int)
     */
    public final void setActualChunkSize(int actualChunkSize) {
        getConfig().actualChunkSize = actualChunkSize;
    }

    /**
     * Set how many chunks a single entry inserted into collection instances created
     * by this object could take. An attempt to insert a larger entry causes
     * {@link IllegalStateException}. This is useful as a self-check that chunk size is configured
     * correctly and that keys and values take expected number of bytes.
     * <p>For example, if {@link #setConstantKeySizeBySample(Object)} is configured or key
     * size is statically known to be constant (boxed primitives, data value generated implementations,
     * {@link Byteable}s, etc.), and the same is true for value objects, max chunks per entry is
     * configured to 1, to ensure keys and values are actually constantly-sized.</p>
     *
     * @param maxChunksPerEntry how many chunks a single entry could span at most
     */
    public final void setMaxChunksPerEntry(int maxChunksPerEntry) {
        getConfig().maxChunksPerEntry = maxChunksPerEntry;
    }

    /**
     * Set timeout of locking on segments of collection instances created by this object,
     * when performing any queries, as well as bulk operations like iteration.
     * <h2>Timeout expression</h2>
     * <p>Valid timeout expressions are composed of a long amount and a standard time unit
     * abbreviation. Examples:</p>
     * <pre>
     * 100ns - 100 nanoseconds
     * 300µs - 300 microseconds
     * 500ms - 500 milliseconds
     *   10s -  10 seconds
     *    2m -   2 minutes
     *    1h -   1 hour
     * </pre>
     *
     * @param lockTimeOut lock timeout expression
     * @see LockTimeOutParser
     */
    public final void setLockTimeOut(String lockTimeOut) {
        if (null != lockTimeOut) {
            getConfig().lockTimeOutParser = new LockTimeOutParser(lockTimeOut);
        }
    }

    /**
     * Set serializer used to serialize both keys and values in ChronicleMap instances created
     * by this object, if they require:
     * <ul>
     *     <li>loose typing</li>
     *     <li>nullability</li>
     * </ul>
     * <p>Set this only if custom {@code key} and {@code value} marshallers are not
     * configured.</p>
     *
     * @param objectSerializer generic serializer of entry keys and values
     */
    public void setObjectSerializer(ObjectSerializer objectSerializer) {
        getConfig().objectSerializer = objectSerializer;
    }

    /**
     * Set listener to be fired on error events in collection instances created by this object.
     *
     * @param errorListener error event listener
     */
    public final void setErrorListener(ChronicleHashErrorListener errorListener) {
        getConfig().errorListener = errorListener;
    }

    /**
     * Set filesystem location to which the collection instance will persist its entries
     * off-heap. The specified value must resolve to a {@code File} that is readable and
     * writable.
     *
     * @param persistedTo off-heap entry storage filesystem location
     */
    public final void setPersistedTo(Resource persistedTo) {
        if (persistedTo instanceof FileSystemResource) {
            setPersistedTo(((FileSystemResource)persistedTo).getFile());
        } else {
            throw new IllegalArgumentException("Resource argument must resolve to a filesystem path");
        }
    }

    /**
     * Set filesystem location to which the collection instance will persist its entries
     * off-heap. The specified value must be a {@code File} that is readable and
     * writable.
     *
     * @param persistedTo off-heap entry storage filesystem location
     */
    public final void setPersistedTo(File persistedTo) {
        getConfig().persistedTo = persistedTo;
    }

    /**
     * Toggle whether key objects of entries in collection instances created by this
     * object are inherently immutable.
     *
     * @param immutableKeys flag indicating whether entry keys are immutable
     * @see ChronicleHashBuilder#immutableKeys()
     */
    public final void setImmutableKeys(boolean immutableKeys) {
        getConfig().immutableKeys = immutableKeys;
    }

    /**
     * Set number of bytes allocated for metadata per entry in collection instances created by this object.
     * The value must be in the range <em>[0..255]</em>.
     *
     * @param metaDataBytes number of bytes allocated for metadata per map entry
     */
    public final void setMetaDataBytes(int metaDataBytes) {
        getConfig().metaDataBytes = metaDataBytes;
    }

    protected abstract <C extends AbstractBuilderConfig> C getConfig();

    /**
     * Abstract superclass of builder configuration types, used internally by
     * concrete subclasses of {@linkplain AbstractChronicleCollectionBuilderBean}. Classes
     * which extend this class gain all fields here, as well as provide any
     * specific configuration settings necessary for their associated Chronicle
     * collection type.
     */
    protected class AbstractBuilderConfig {

        public Class<K> keyClass = null;
        public BytesMarshaller<? super K> keyMarshaller;
        public Double averageKeySize = null;
        public K sampleKey = null;

        public long maxEntries = -1L;
        public Boolean immutableKeys;
        public int metaDataBytes = -1;

        public long entriesPerSegment = -1L;
        public int actualSegments = -1;
        public int minSegments = -1;
        public long actualChunksPerSegment = -1L;
        public int actualChunkSize = -1;
        public int maxChunksPerEntry = -1;

        public ObjectSerializer objectSerializer;

        private LockTimeOutParser lockTimeOutParser = null;
        public File persistedTo = null;

        public ChronicleHashErrorListener errorListener = null;

        public void checkKeySizing() {
            if (null != averageKeySize && -1.0 == Math.signum(averageKeySize)) {
                throw new IllegalArgumentException("Average key size must be positive number");
            }
            if (null != averageKeySize && null != sampleKey) {
                throw new IllegalStateException("Ambiguous key sizing: average and constant sizes specified");
            }
        }

        public boolean isLockTimeOutSet() {
            return (null != lockTimeOutParser && lockTimeOutParser.valid());
        }

        public long getLockTimeOutAmount() {
            return lockTimeOutParser.getAmount();
        }

        public TimeUnit getLockTimeOutUnit() {
            return lockTimeOutParser.getUnit();
        }

    }

}
