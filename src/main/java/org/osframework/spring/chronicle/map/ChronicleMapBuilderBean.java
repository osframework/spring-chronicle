package org.osframework.spring.chronicle.map;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import net.openhft.lang.io.serialization.BytesMarshaller;
import net.openhft.lang.model.Byteable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.Resource;

import java.io.File;

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
     * Set maximum number of entries contained by the ChronicleMap instance this object builds.
     *
     * @param maxEntries maximum number of map entries
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
        config.persistedTo = persistedTo;
    }

    public void setPutReturnsNull(boolean putReturnsNull) {
        config.putReturnsNull = putReturnsNull;
    }

    public void setRemoveReturnsNull(boolean removeReturnsNull) {
        config.removeReturnsNull = removeReturnsNull;
    }

    public void setImmutableKeys(boolean immutableKeys) {
        config.immutableKeys = immutableKeys;
    }

    @Override
    public Class<?> getObjectType() {
        return ChronicleMap.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (null == config.keyClass) {
            throw new IllegalStateException("Map key class must be specified prior to ChronicleMap construction");
        }
        if (null == config.valueClass) {
            throw new IllegalStateException("Map value class must be specified prior to ChronicleMap construction");
        }
        if (null != config.persistedTo) {
            File f = config.persistedTo.getFile();
            if (!f.canRead() || !f.canWrite()) {
                throw new IllegalStateException("Off-heap persistence file must be readable and writable");
            }
        }
        super.afterPropertiesSet();
    }

    @Override
    protected ChronicleMap<K, V> createInstance() throws Exception {
        builder = ChronicleMapBuilder.of(config.keyClass, config.valueClass);
        slf4jLogger.info("Constructing {} of ChronicleMap<{}, {}>",
                (isSingleton() ? "singleton instance" : "instances"),
                config.keyClass.getSimpleName(),
                config.valueClass.getSimpleName());
        if (0L >= config.maxEntries) {
            slf4jLogger.info("Map maximum entries unspecified; using default...");
            config.maxEntries = DEFAULT_ENTRIES;
        }
        builder.entries(config.maxEntries);
        slf4jLogger.info("Map will hold maximum of {} entries", config.maxEntries);
        if (null != config.keyMarshaller) {
            slf4jLogger.info("Map entry keys serialized by {}", config.keyMarshaller.getClass().getSimpleName());
            builder.keyMarshaller(config.keyMarshaller);
        }
        if (null != config.valueMarshaller) {
            slf4jLogger.info("Map entry values serialized by {}", config.valueMarshaller.getClass().getSimpleName());
            builder.valueMarshaller(config.valueMarshaller);
        }
        if (Boolean.TRUE.equals(config.immutableKeys)) {
            slf4jLogger.info("Map will employ immutable keys");
            builder.immutableKeys();
        }
        if (Boolean.TRUE.equals(config.putReturnsNull)) {
            slf4jLogger.info("Invocation of put method will return null");
            builder.putReturnsNull(config.putReturnsNull);
        }
        if (Boolean.TRUE.equals(config.removeReturnsNull)) {
            slf4jLogger.info("Invocation of remove method will return null");
            builder.removeReturnsNull(config.removeReturnsNull);
        }
        if (null != config.lockTimeOutParser && config.lockTimeOutParser.valid()) {
            slf4jLogger.info("Map query operation lock timeout set to {} {}",
                             config.lockTimeOutParser.getAmount(),
                             config.lockTimeOutParser.getUnit());
            builder.lockTimeOut(config.lockTimeOutParser.getAmount(), config.lockTimeOutParser.getUnit());
        }

        if (null != config.persistedTo) {
            slf4jLogger.info("Map entries persisted off-heap at {}", config.persistedTo.toString());
        }
        return (null != config.persistedTo) ? builder.createPersistedTo(config.persistedTo.getFile()) : builder.create();
    }

    /**
     * Holds configuration values passed to parent {@code ChronicleMapBuilderBean} mutator methods. Allows for delayed
     * construction of the {@code ChronicleMapBuilder<K, V>} instance.
     */
    final class ChronicleMapBuilderConfig {

        private Class<K> keyClass;
        private double averageKeySize;
        private K sampleKey;

        private Class<V> valueClass;
        private double averageValueSize;
        private V sampleValue;

        private long maxEntries = -1L;
        private BytesMarshaller<? super K> keyMarshaller;
        private BytesMarshaller<? super V> valueMarshaller;
        private Boolean putReturnsNull, removeReturnsNull, immutableKeys;
        private LockTimeOutParser lockTimeOutParser = null;
        private Resource persistedTo;

        private void checkKeySizing() {
            if (-1.0 == Math.signum(averageKeySize)) {
                throw new IllegalArgumentException("Average key size must be positive number");
            }
            if (1.0 == Math.signum(averageKeySize) && null != sampleKey) {
                throw new IllegalStateException("Ambiguous key sizing: average and constant sizes specified");
            }
        }

        private void checkValueSizing() {
            if (-1.0 == Math.signum(averageValueSize)) {
                throw new IllegalArgumentException("Average value size must be positive number");
            }
            if (1.0 == Math.signum(averageValueSize) && null != sampleValue) {
                throw new IllegalStateException("Ambiguous value sizing: average and constant sizes specified");
            }
        }
    }

}
