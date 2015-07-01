package org.osframework.spring.chronicle.set;

import net.openhft.chronicle.set.ChronicleSet;
import net.openhft.chronicle.set.ChronicleSetBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;

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
public class ChronicleSetBuilderBean<K> extends AbstractFactoryBean<ChronicleSet<K>> {

    private final ChronicleSetBuilderConfig config;
    private final Logger slf4jLogger;

    private ChronicleSetBuilder<K> builder = null;

    public ChronicleSetBuilderBean() {
        super();
        config = new ChronicleSetBuilderConfig();
        slf4jLogger = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Set key type of the ChronicleSet instance this object builds.
     *
     * @param keyClass class of the set value type
     */
    public void setKeyClass(Class<K> keyClass) {
        config.keyClass = keyClass;
    }

    /**
     * Set filesystem location to which the ChronicleSet instance will persist its entries off-heap. The
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
     * Set filesystem location to which the ChronicleSet instance will persist its entries off-heap. The
     * specified value must be a {@code File} that is readable and writable.
     *
     * @param persistedTo off-heap entry storage filesystem location
     */
    public void setPersistedTo(File persistedTo) {
        config.persistedTo = persistedTo;
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

        if (null != config.persistedTo) {
            slf4jLogger.info("Set entries persisted off-heap at {}", config.persistedTo.toString());
        }
        return (null != config.persistedTo) ? builder.createPersistedTo(config.persistedTo) : builder.create();
    }

    /**
     * Holds configuration values passed to parent {@code ChronicleSetBuilderBean} mutator
     * methods. Allows for delayed construction of the {@code ChronicleSetBuilder<K>} instance.
     */
    final class ChronicleSetBuilderConfig {

        private Class<K> keyClass;

        private File persistedTo = null;
    }

}
