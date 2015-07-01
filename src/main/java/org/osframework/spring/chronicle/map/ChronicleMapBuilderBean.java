package org.osframework.spring.chronicle.map;

import net.openhft.chronicle.hash.ChronicleHashBuilder;
import net.openhft.chronicle.hash.ChronicleHashErrorListener;
import net.openhft.chronicle.map.*;
import net.openhft.lang.io.serialization.BytesMarshaller;
import net.openhft.lang.io.serialization.ObjectSerializer;
import net.openhft.lang.model.Byteable;
import org.osframework.spring.chronicle.InetSocketAddressEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;

import static org.springframework.util.Assert.notNull;

/**
 * Adapter implementation of {@code FactoryBean} interface to support creation of a
 * {@code ChronicleMap} object through a {@code ChronicleMapBuilder} instance. This class
 * adapts the <em>fluent API</em> builder methods of {@code ChronicleMapBuilder} to the
 * standard JavaBean API mutator style, for declarative configuration of the details of the
 * created map in a Spring BeanFactory.
 * <p>On initialization of the BeanFactory, an instance of this class produces a singleton {@code ChronicleMapBuilder}
 * object, which is thread safe. The underlying singleton builder can be safely used to create both singleton and
 * prototype scope {@code ChronicleMap} objects.</p>
 *
 * @param <K> Key class type parameter of ChronicleMap to be built
 * @param <V> Value class type parameter of ChronicleMap to be built
 *
 * @author <a href="mailto:dave@osframework.org">Dave Joyce</a>
 * @since 0.0.1
 * @see net.openhft.chronicle.map.ChronicleMap
 * @see net.openhft.chronicle.map.ChronicleMapBuilder
 */
public class ChronicleMapBuilderBean<K, V> extends AbstractFactoryBean<ChronicleMap<K, V>> {

    protected static final long DEFAULT_ENTRIES = 1 << 20;

    private final ChronicleMapBuilderConfig config;
    private final Logger slf4jLogger;

    private ChronicleMapBuilder<K, V> builder = null;

    public ChronicleMapBuilderBean() {
        super();
        config = new ChronicleMapBuilderConfig();
        slf4jLogger = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Set key type of the ChronicleMap instance this object builds.
     *
     * @param keyClass class of the map key type
     */
    public void setKeyClass(Class<K> keyClass) {
        config.keyClass = keyClass;
    }

    /**
     * Set the {@code BytesMarshaller} used to serialize/deserialize keys to/from off-heap
     * memory in the ChronicleMap instance created by this object. See
     * <a href="https://github.com/OpenHFT/Chronicle-Map#serialization">the section about serialization in ChronicleMap
     * manual</a> for more information.
     *
     * @param keyMarshaller marshaller used to serialize keys
     * @see #setValueMarshaller(BytesMarshaller)
     */
    public void setKeyMarshaller(BytesMarshaller<? super K> keyMarshaller) {
        config.keyMarshaller = keyMarshaller;
    }

    /**
     * Set the average number of bytes, taken by serialized form of keys, put into the
     * ChronicleMap instance this object builds. If key size is always the same, call
     * {@link #setConstantKeySizeBySample(Object)} method instead of this one.
     * <p>If key is a boxed primitive type or {@code Byteable} subclass, i. e. if key size is known
     * statically, it is automatically accounted and shouldn't be specified by user.</p>
     *
     * @param averageKeySize average number of bytes, taken by serialized form of keys
     * @throws IllegalArgumentException if argument is not positive number
     * @throws IllegalStateException if {@link #setConstantKeySizeBySample(Object) sample key} has been specified
     */
    public void setAverageKeySize(double averageKeySize) {
        config.averageKeySize = averageKeySize;
        config.checkKeySizing();
    }

    /**
     * Set the constant number of bytes, taken by serialized form of keys, put into the
     * ChronicleMap instance this object builds. If key size varies, call
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
        config.sampleKey = sampleKey;
        config.checkKeySizing();
    }

    /**
     * Set value type of the ChronicleMap instance this object builds.
     *
     * @param valueClass class of the map value type
     */
    public void setValueClass(Class<V> valueClass) {
        config.valueClass = valueClass;
    }

    /**
     * Set the {@code BytesMarshaller} used to serialize/deserialize values to/from off-heap
     * memory in the ChronicleMap instance created by this object. See
     * <a href="https://github.com/OpenHFT/Chronicle-Map#serialization">the section about serialization in ChronicleMap
     * manual</a> for more information.
     *
     * @param valueMarshaller marshaller used to serialize values
     * @see #setKeyMarshaller(BytesMarshaller)
     */
    public void setValueMarshaller(BytesMarshaller<? super V> valueMarshaller) {
        config.valueMarshaller = valueMarshaller;
    }

    /**
     * Set the average number of bytes, taken by serialized form of values, put into the
     * ChronicleMap instance this object builds. If value size is always the same, call
     * {@link #setConstantValueSizeBySample(Object)} method instead of this one.
     * <p>If value is a boxed primitive type or {@code Byteable} subclass, i. e. if value size is known
     * statically, it is automatically accounted and shouldn't be specified by user.</p>
     *
     * @param averageValueSize average number of bytes, taken by serialized form of values
     */
    public void setAverageValueSize(double averageValueSize) {
        config.averageValueSize = averageValueSize;
        config.checkValueSizing();
    }

    /**
     * Set the constant number of bytes, taken by serialized form of values, put into the
     * ChronicleMap instance this object builds. If value size varies, call
     * {@link #setAverageValueSize(double)} method instead of this one. By providing the
     * {@code sampleValue}, all values should take the same number of bytes in serialized form,
     * as this sample object.
     * <p>If value is a boxed primitive type or {@code Byteable} subclass, i. e. if value size is known
     * statically, it is automatically accounted and shouldn't be specified by user.</p>
     *
     * @param sampleValue sample value with constant number of bytes for all values
     */
    public void setConstantValueSizeBySample(V sampleValue) {
        config.sampleValue = sampleValue;
        config.checkValueSizing();
    }

    /**
     * Set the size in bytes of the allocation unit of ChronicleMap instances this object builds.
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
    public void setActualChunkSize(int actualChunkSize) {
        config.actualChunkSize = actualChunkSize;
    }

    /**
     * Set how many chunks a single entry inserted into {@code ChronicleMap} instances created
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
    public void setMaxChunksPerEntry(int maxChunksPerEntry) {
        config.maxChunksPerEntry = maxChunksPerEntry;
    }

    /**
     * Set alignment strategy of memeory address of entries and independently of memory address of
     * values within entries in ChronicleMap instances created by this object.
     *
     * @param alignment new alignment of the maps constructed by this object
     */
    public void setEntryAndValueAlignment(Alignment alignment) {
        config.alignment = alignment;
    }

    /**
     * Set alignment strategy of memeory address of entries and independently of memory address of
     * values within entries in ChronicleMap instances created by this object.
     * <p>This method converts the specified text to its {@code Alignment} value and then
     * delegates to {@link #setEntryAndValueAlignment(Alignment)}.</p>
     *
     * @param alignment new alignment of the maps constructed by this object
     * @throws IllegalArgumentException if text cannot be converted to an Alignment value
     */
    public void setEntryAndValueAlignment(String alignment) {
        AlignmentEditor propertyEditor = new AlignmentEditor();
        propertyEditor.setAsText(alignment);
        setEntryAndValueAlignment((Alignment) propertyEditor.getValue());
    }

    /**
     * Set actual maximum number of entries that could be inserted into any single segment
     * of ChronicleMap instances created by this object. Configuring both the actual number of
     * entries per segment and {@link #setActualSegments(int) actual segments} replaces a
     * single {@link #setMaxEntries(long)} configuration.
     * <p>This is a <em>low-level configuration</em>.</p>
     *
     * @param entriesPerSegment actual maximum number of entries per segment in maps constructed by this object
     * @see #setMaxEntries(long)
     * @see #setActualSegments(int)
     */
    public void setEntriesPerSegment(long entriesPerSegment) {
        config.entriesPerSegment = entriesPerSegment;
    }

    /**
     * Set the actual number of chunks that will be reserved for any single segment of the
     * ChronicleMap instances created by this object. This configuration is a lower-level version of
     * {@link #setEntriesPerSegment(long)}.
     * <p>Setting this property makes sense only if {@link #setActualChunkSize(int)},
     * {@link #setActualSegments(int)}, and {@link #setEntriesPerSegment(long)} are also configured
     * manually.</p>
     *
     * @param actualChunksPerSegment the actual number of chunks reserved per segment in the
     *                               ChronicleMap instances created by this object
     */
    public void setActualChunksPerSegment(long actualChunksPerSegment) {
        config.actualChunksPerSegment = actualChunksPerSegment;
    }

    /**
     * Set the actual number of segments in ChronicleMap instances created by this object.
     * With {@link #setEntriesPerSegment(long) actual number of segments}, this configuration
     * replaces a single {@link #setMaxEntries(long)} call.
     * <p>This is a <em>low-level configuration</em>.</p>
     *
     * @param actualSegments actual number of segments in maps constructed by this object
     * @see #setEntriesPerSegment(long)
     */
    public void setActualSegments(int actualSegments) {
        config.actualSegments = actualSegments;
    }

    /**
     * Set minimum number of segments in ChronicleMap instances created by this object.
     *
     * @param minSegments minimum number of segments in maps constructed by this object
     */
    public void setMinSegments(int minSegments) {
        config.minSegments = minSegments;
    }

    /**
     * Set maximum number of entries contained by the ChronicleMap instances created by this object.
     *
     * @param maxEntries maximum number of map entries
     * @see net.openhft.chronicle.hash.ChronicleHashBuilder#entries(long)
     */
    public void setMaxEntries(long maxEntries) {
        config.maxEntries = maxEntries;
    }

    /**
     * Set timeout of locking on segments of {@code ChronicleMap} objects, created by this factory bean, when performing
     * any queries, as well as bulk operations like iteration.
     * <h2>Timeout expression</h2>
     * <p>Valid timeout expressions are composed of a long amount and a standard time unit abbreviation. Examples:</p>
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
    public void setLockTimeOut(String lockTimeOut) {
        if (null != lockTimeOut) {
            config.lockTimeOutParser = new LockTimeOutParser(lockTimeOut);
        }
    }

    /**
     * Set filesystem location to which the {@code ChronicleMap} instance will persist its entries off-heap. The
     * specified value must resolve to a {@code File} that is readable and writable.
     *
     * @param persistedTo off-heap entry storage filesystem location
     */
    public void setPersistedTo(Resource persistedTo) {
        if (persistedTo instanceof FileSystemResource) {
            setPersistedTo(((FileSystemResource)persistedTo).getFile());
        } else {
            throw new IllegalArgumentException("Resource argument must resolve to a filesystem path");
        }
    }

    /**
     * Set filesystem location to which the {@code ChronicleMap} instance will persist its entries off-heap. The
     * specified value must be a {@code File} that is readable and writable.
     *
     * @param persistedTo off-heap entry storage filesystem location
     */
    public void setPersistedTo(File persistedTo) {
        config.persistedTo = persistedTo;
    }

    /**
     * Set network addresses to which ChronicleMap instances created by this object will push entries.
     *
     * @param pushToAddresses network addresses to push to
     */
    public void setPushTo(InetSocketAddress... pushToAddresses) {
        if (0 < pushToAddresses.length) {
            config.pushToAddresses = pushToAddresses;
        }
    }

    /**
     * Set network addresses to which ChronicleMap instances created by this object will push entries.
     *
     * @param pushToAddresses network addresses to push to
     * @see InetSocketAddressEditor
     */
    public void setPushTo(String... pushToAddresses) {
        InetSocketAddress[] converted = new InetSocketAddress[pushToAddresses.length];
        for (int i = 0; i < pushToAddresses.length; i++) {
            InetSocketAddressEditor editor = new InetSocketAddressEditor();
            editor.setAsText(pushToAddresses[i]);
            converted[i] = (InetSocketAddress)editor.getValue();
        }
        setPushTo(converted);
    }

    /**
     * Set listener to be fired on error events in ChronicleMap instances created by this object.
     *
     * @param errorListener error event listener
     */
    public void setErrorListener(ChronicleHashErrorListener errorListener) {
        config.errorListener = errorListener;
    }

    /**
     * Set listener to be fired when key events occur in ChronicleMap instances created by
     * this object.
     *
     * @param eventListener key event listener
     */
    public void setEventListener(MapEventListener<K, V> eventListener) {
        config.eventListener = eventListener;
    }

    /**
     * Set serialized bytes listener to be fired when key events occur in ChronicleMap
     * instances created by this object.
     *
     * @param bytesEventListener key event listener
     * @see BytesMapEventListener
     */
    public void setBytesMapEventListener(BytesMapEventListener bytesEventListener) {
        config.bytesEventListener = bytesEventListener;
    }

    /**
     * Set serializer used to serialize both keys and values in ChronicleMap instances created
     * by this object, if they require:
     * <ul>
     *     <li>loose typing</li>
     *     <li>nullability</li>
     * </ul>
     * <p>Set this only if custom {@link #setKeyMarshaller(BytesMarshaller) key} and
     * {@link #setValueMarshaller(BytesMarshaller) value} marshallers are not configured.</p>
     *
     * @param objectSerializer generic serializer of entry keys and values
     */
    public void setObjectSerializer(ObjectSerializer objectSerializer) {
        config.objectSerializer = objectSerializer;
    }

    /**
     * Toggle behavior of ChronicleMap instances created by this object, when the
     * {@link java.util.Map#put(Object, Object)} method is called.
     *
     * @param putReturnsNull flag indicating whether {@code put} method returns null
     */
    public void setPutReturnsNull(boolean putReturnsNull) {
        config.putReturnsNull = putReturnsNull;
    }

    /**
     * Toggle behavior of ChronicleMap instances created by this object, when the
     * {@link java.util.Map#remove(Object)} method is called.
     *
     * @param removeReturnsNull flag indicating whether {@code remove} method returns null
     */
    public void setRemoveReturnsNull(boolean removeReturnsNull) {
        config.removeReturnsNull = removeReturnsNull;
    }

    /**
     * Toggle whether key objects of entries in ChronicleMap instances created by this
     * object are inherently immutable.
     *
     * @param immutableKeys flag indicating whether entry keys are immutable
     * @see ChronicleHashBuilder#immutableKeys()
     */
    public void setImmutableKeys(boolean immutableKeys) {
        config.immutableKeys = immutableKeys;
    }

    /**
     * Set number of bytes allocated for metadata per entry in ChronicleMap instances created by this object.
     * The value must be in the range <em>[0..255]</em>.
     *
     * @param metaDataBytes number of bytes allocated for metadata per map entry
     */
    public void setMetaDataBytes(int metaDataBytes) {
        config.metaDataBytes = metaDataBytes;
    }

    /**
     * Get the type of object that this {@code FactoryBean} creates.
     *
     * @return {@code ChronicleMap} class
     */
    @Override
    public Class<?> getObjectType() {
        return ChronicleMap.class;
    }

    /**
     * {@inheritDoc}
     * <p>This method implementation validates:</p>
     * <ul>
     *     <li>Required {@code keyClass} and {@code valueClass} are set</li>
     *     <li>The {@code persistedTo} property is readable and writable (if set)</li>
     * </ul>
     *
     * @throws Exception if any validation fails prior to map creation
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (null == config.keyClass) {
            throw new IllegalStateException("Map key class must be specified prior to ChronicleMap construction");
        }
        if (null == config.valueClass) {
            throw new IllegalStateException("Map value class must be specified prior to ChronicleMap construction");
        }
        if (null != config.persistedTo) {
            if (config.persistedTo.isDirectory()) {
                throw new IllegalArgumentException("Property 'persistedTo' cannot be a directory");
            }
            if (!config.persistedTo.canRead() || !config.persistedTo.canWrite()) {
                throw new IllegalStateException("Off-heap persistence file must be readable and writable");
            }
        }
        super.afterPropertiesSet();
    }

    /**
     * {@inheritDoc}
     * <p>This method implementation constructs and configures a
     * {@linkplain ChronicleMapBuilder} singleton, from which the {@code ChronicleMap} object
     * is produced.</p>
     *
     * @return constructed, configured {@code ChronicleMap} object
     * @throws Exception if map construction fails for any reason
     */
    @Override
    protected ChronicleMap<K, V> createInstance() throws Exception {
        // 1. Initial builder setup
        builder = ChronicleMapBuilder.of(config.keyClass, config.valueClass);
        slf4jLogger.info("Constructing {} of ChronicleMap<{}, {}>",
                (isSingleton() ? "singleton instance" : "instances"),
                config.keyClass.getSimpleName(),
                config.valueClass.getSimpleName());
        if (0L >= config.maxEntries) {
            config.maxEntries = DEFAULT_ENTRIES;
            slf4jLogger.info("Map maximum entries unspecified; using default...");
        }
        builder.entries(config.maxEntries);
        slf4jLogger.info("Map will hold maximum of {} entries", config.maxEntries);

        // 2. Key settings
        if (null != config.averageKeySize) {
            builder.averageKeySize(config.averageKeySize);
            slf4jLogger.debug("Map entry avg key size: {} bytes", config.averageKeySize);
        }
        if (null != config.sampleKey) {
            builder.constantKeySizeBySample(config.sampleKey);
            slf4jLogger.debug("Map entry const key size set");
        }

        // 3. Value settings
        if (null != config.averageValueSize) {
            builder.averageValueSize(config.averageValueSize);
            slf4jLogger.debug("Map entry avg value size: {} bytes", config.averageValueSize);
        }
        if (null != config.sampleValue) {
            builder.constantValueSizeBySample(config.sampleValue);
            slf4jLogger.debug("Map entry const value size set");
        }

        // 4. Serializer settings
        if (null != config.keyMarshaller) {
            builder.keyMarshaller(config.keyMarshaller);
            slf4jLogger.debug("Map entry keys serialized by {}", config.keyMarshaller.getClass().getSimpleName());
        }
        if (null != config.valueMarshaller) {
            builder.valueMarshaller(config.valueMarshaller);
            slf4jLogger.debug("Map entry values serialized by {}", config.valueMarshaller.getClass().getSimpleName());
        }
        if (null != config.objectSerializer) {
            builder.objectSerializer(config.objectSerializer);
            slf4jLogger.debug("Map entries serialized by {}", config.objectSerializer.getClass().getSimpleName());
        }

        // 5. Entry operation behavior settings
        if (Boolean.TRUE.equals(config.immutableKeys)) {
            builder.immutableKeys();
            slf4jLogger.debug("Map will employ immutable keys");
        }
        if (Boolean.TRUE.equals(config.putReturnsNull)) {
            builder.putReturnsNull(config.putReturnsNull);
            slf4jLogger.debug("Invocation of put method will return null");
        }
        if (Boolean.TRUE.equals(config.removeReturnsNull)) {
            builder.removeReturnsNull(config.removeReturnsNull);
            slf4jLogger.debug("Invocation of remove method will return null");
        }
        if (-1 != config.metaDataBytes) {
            builder.metaDataBytes(config.metaDataBytes);
            slf4jLogger.debug("Map entries will allocate {} bytes for metadata", config.metaDataBytes);
        }
        if (null != config.lockTimeOutParser && config.lockTimeOutParser.valid()) {
            builder.lockTimeOut(config.lockTimeOutParser.getAmount(), config.lockTimeOutParser.getUnit());
            slf4jLogger.debug("Map query operation lock timeout set to {} {}",
                              config.lockTimeOutParser.getAmount(),
                              config.lockTimeOutParser.getUnit());
        }

        // 6. Low-level storage settings
        if (null != config.alignment) {
            builder.entryAndValueAlignment(config.alignment);
            slf4jLogger.debug("Map entry / value alignment set to {}", config.alignment);
        }
        if (-1 != config.actualChunkSize) {
            builder.actualChunkSize(config.actualChunkSize);
            slf4jLogger.debug("Map will allocate chunks of {} bytes", config.actualChunkSize);
        }
        if (-1 != config.maxChunksPerEntry) {
            builder.maxChunksPerEntry(config.maxChunksPerEntry);
            slf4jLogger.debug("Map will use max of {} chunks per entry", config.maxChunksPerEntry);
        }
        if (-1 != config.minSegments) {
            builder.minSegments(config.minSegments);
            slf4jLogger.debug("Map will have min of {} segments", config.minSegments);
        }
        if (-1 != config.actualSegments) {
            builder.actualSegments(config.actualSegments);
            slf4jLogger.debug("Map will have {} actual segments", config.actualSegments);
        }
        if (-1L != config.entriesPerSegment) {
            builder.entriesPerSegment(config.entriesPerSegment);
            slf4jLogger.debug("Map will store {} entries per segment", config.entriesPerSegment);
        }
        if (-1L != config.actualChunksPerSegment) {
            builder.actualChunksPerSegment(config.actualChunksPerSegment);
            slf4jLogger.debug("Map will allocate {} chunks per segment", config.actualChunksPerSegment);
        }

        // 7. Event listener settings
        if (null != config.errorListener) {
            builder.errorListener(config.errorListener);
            slf4jLogger.debug("Map error listener: {}", config.errorListener.getClass().getSimpleName());
        }
        if (null != config.eventListener) {
            builder.eventListener(config.eventListener);
            slf4jLogger.debug("Map event listener: {}", config.eventListener.getClass().getSimpleName());
        }
        if (null != config.bytesEventListener) {
            builder.bytesEventListener(config.bytesEventListener);
            slf4jLogger.debug("Map bytes event listener: {}", config.bytesEventListener.getClass().getSimpleName());
        }

        // 8. Network & replication settings
        if (null != config.pushToAddresses && 0 < config.pushToAddresses.length) {
            builder.pushTo(config.pushToAddresses);
            slf4jLogger.debug("Map entries will push to [{}]", Arrays.toString(config.pushToAddresses));
        }

        if (null != config.persistedTo) {
            slf4jLogger.info("Map entries persisted off-heap at {}", config.persistedTo.toString());
        }
        return (null != config.persistedTo) ? builder.createPersistedTo(config.persistedTo) : builder.create();
    }

    /**
     * Holds configuration values passed to parent {@code ChronicleMapBuilderBean} mutator methods. Allows for delayed
     * construction of the {@code ChronicleMapBuilder<K, V>} instance.
     */
    final class ChronicleMapBuilderConfig {

        private Class<K> keyClass;
        private BytesMarshaller<? super K> keyMarshaller;
        private Double averageKeySize = null;
        private K sampleKey = null;

        private Class<V> valueClass;
        private BytesMarshaller<? super V> valueMarshaller;
        private Double averageValueSize = null;
        private V sampleValue = null;

        private int actualChunkSize = -1;
        private int maxChunksPerEntry = -1;
        private Alignment alignment = null;
        private long maxEntries = -1L;
        private long entriesPerSegment = -1L;
        private int actualSegments = -1;
        private long actualChunksPerSegment = -1L;
        private int minSegments = -1;

        private ObjectSerializer objectSerializer;
        private Boolean putReturnsNull, removeReturnsNull, immutableKeys;
        private int metaDataBytes = -1;
        private LockTimeOutParser lockTimeOutParser = null;
        private File persistedTo = null;
        private InetSocketAddress[] pushToAddresses = null;
        private ChronicleHashErrorListener errorListener = null;
        private MapEventListener<K, V> eventListener = null;
        private BytesMapEventListener bytesEventListener = null;

        private void checkKeySizing() {
            if (null != averageKeySize && -1.0 == Math.signum(averageKeySize)) {
                throw new IllegalArgumentException("Average key size must be positive number");
            }
            if (null != averageKeySize && null != sampleKey) {
                throw new IllegalStateException("Ambiguous key sizing: average and constant sizes specified");
            }
        }

        private void checkValueSizing() {
            if (null != averageValueSize && -1.0 == Math.signum(averageValueSize)) {
                throw new IllegalArgumentException("Average value size must be positive number");
            }
            if (null != averageValueSize && null != sampleValue) {
                throw new IllegalStateException("Ambiguous value sizing: average and constant sizes specified");
            }
        }
    }

}
