package org.osframework.spring.chronicle.set;

import net.openhft.chronicle.hash.ChronicleHashBuilder;
import net.openhft.chronicle.set.ChronicleSet;
import net.openhft.chronicle.set.ChronicleSetBuilder;
import org.osframework.spring.chronicle.AbstractChronicleBuilderBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.Arrays;

/**
 * Adapter implementation of {@code FactoryBean} interface to support creation of a
 * {@code ChronicleSet} object through a {@code ChronicleSetBuilder} instance. This class
 * adapts the <em>fluent API</em> builder methods of {@code ChronicleSetBuilder} to the
 * standard JavaBean API mutator style, for declarative configuration of the details of the
 * created set in a Spring BeanFactory.
 * <p>On initialization of the BeanFactory, an instance of this class produces a singleton
 * {@code ChronicleSetBuilder} object, which is thread safe. The underlying singleton
 * builder can be safely used to create both singleton and prototype scope
 * {@code ChronicleSet} objects.</p>
 *
 * @param <K> Key class of ChronicleSet to be built
 *
 * @author <a href="mailto:dave@osframework.org">Dave Joyce</a>
 * @since 0.0.1
 * @see net.openhft.chronicle.set.ChronicleSet
 * @see net.openhft.chronicle.set.ChronicleSetBuilder
 */
public class ChronicleSetBuilderBean<K> extends AbstractChronicleBuilderBean<K, ChronicleSet<K>> {

    private final ChronicleSetBuilderConfig config;

    private ChronicleSetBuilder<K> builder = null;

    public ChronicleSetBuilderBean() {
        super();
        config = new ChronicleSetBuilderConfig();
    }

    /**
     * Get the type of object that this {@code FactoryBean} creates.
     *
     * @return {@code ChronicleSet} class
     */
    @Override
    public Class<?> getObjectType() {
        return ChronicleSet.class;
    }

    /**
     * {@inheritDoc}
     * <p>This method implementation validates:</p>
     * <ul>
     *     <li>Required {@code keyClass} is set</li>
     *     <li>The {@code persistedTo} property is readable and writable (if set)</li>
     * </ul>
     *
     * @throws Exception if any validation fails prior to set creation
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (null == config.keyClass) {
            throw new IllegalStateException("Set key class must be specified prior to ChronicleSet construction");
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
     */
    protected ChronicleSetBuilderConfig getConfig() {
        return config;
    }

    /**
     * {@inheritDoc}
     * <p>This method implementation constructs and configures a
     * {@linkplain ChronicleSetBuilder} singleton, from which the {@code ChronicleSet} object
     * is produced.</p>
     *
     * @return constructed, configured {@code ChronicleSet} object
     * @throws Exception if set construction fails for any reason
     */
    @Override
    protected ChronicleSet<K> createInstance() throws Exception {
        // 1. Initial builder setup
        builder = ChronicleSetBuilder.of(config.keyClass);
        slf4jLogger.info("Constructing {} of ChronicleSet<{}>",
                (isSingleton() ? "singleton instance" : "instances"),
                config.keyClass.getSimpleName());
        if (0L >= config.maxEntries) {
            config.maxEntries = DEFAULT_ENTRIES;
            slf4jLogger.info("Set maximum entries unspecified; using default...");
        }
        builder.entries(config.maxEntries);
        slf4jLogger.info("Set will hold maximum of {} entries", config.maxEntries);

        // 2. Key settings
        if (null != config.averageKeySize) {
            builder.averageKeySize(config.averageKeySize);
            slf4jLogger.debug("Set entry avg key size: {} bytes", config.averageKeySize);
        }
        if (null != config.sampleKey) {
            builder.constantKeySizeBySample(config.sampleKey);
            slf4jLogger.debug("Set entry const key size set");
        }

        // 4. Serializer settings
        if (null != config.keyMarshaller) {
            builder.keyMarshaller(config.keyMarshaller);
            slf4jLogger.debug("Set entry keys serialized by {}", config.keyMarshaller.getClass().getSimpleName());
        }
        if (null != config.objectSerializer) {
            builder.objectSerializer(config.objectSerializer);
            slf4jLogger.debug("Set entries serialized by {}", config.objectSerializer.getClass().getSimpleName());
        }

        // 5. Entry operation behavior settings
        if (Boolean.TRUE.equals(config.immutableKeys)) {
            builder.immutableKeys();
            slf4jLogger.debug("Set will employ immutable keys");
        }
        if (-1 != config.metaDataBytes) {
            builder.metaDataBytes(config.metaDataBytes);
            slf4jLogger.debug("Set entries will allocate {} bytes for metadata", config.metaDataBytes);
        }
        if (config.isLockTimeOutSet()) {
            builder.lockTimeOut(config.getLockTimeOutAmount(), config.getLockTimeOutUnit());
            slf4jLogger.debug("Set query operation lock timeout set to {} {}",
                    config.getLockTimeOutAmount(),
                    config.getLockTimeOutUnit());
        }

        // 6. Low-level storage settings
        if (-1 != config.actualChunkSize) {
            builder.actualChunkSize(config.actualChunkSize);
            slf4jLogger.debug("Set will allocate chunks of {} bytes", config.actualChunkSize);
        }
        if (-1 != config.maxChunksPerEntry) {
            builder.maxChunksPerEntry(config.maxChunksPerEntry);
            slf4jLogger.debug("Set will use max of {} chunks per entry", config.maxChunksPerEntry);
        }
        if (-1 != config.minSegments) {
            builder.minSegments(config.minSegments);
            slf4jLogger.debug("Set will have min of {} segments", config.minSegments);
        }
        if (-1 != config.actualSegments) {
            builder.actualSegments(config.actualSegments);
            slf4jLogger.debug("Set will have {} actual segments", config.actualSegments);
        }
        if (-1L != config.entriesPerSegment) {
            builder.entriesPerSegment(config.entriesPerSegment);
            slf4jLogger.debug("Set will store {} entries per segment", config.entriesPerSegment);
        }
        if (-1L != config.actualChunksPerSegment) {
            builder.actualChunksPerSegment(config.actualChunksPerSegment);
            slf4jLogger.debug("Set will allocate {} chunks per segment", config.actualChunksPerSegment);
        }

        // 7. Event listener settings
        if (null != config.errorListener) {
            builder.errorListener(config.errorListener);
            slf4jLogger.debug("Set error listener: {}", config.errorListener.getClass().getSimpleName());
        }

        if (null != config.persistedTo) {
            slf4jLogger.info("Set entries persisted off-heap at {}", config.persistedTo.toString());
        }
        return (null != config.persistedTo) ? builder.createPersistedTo(config.persistedTo) : builder.create();
    }

    /**
     * Holds configuration values passed to parent {@code ChronicleSetBuilderBean} mutator
     * methods. Allows for delayed construction of the {@code ChronicleSetBuilder<K>} instance.
     */
    final class ChronicleSetBuilderConfig extends AbstractBuilderConfig {

    }

}
