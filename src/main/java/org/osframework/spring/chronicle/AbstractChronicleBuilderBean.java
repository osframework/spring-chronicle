package org.osframework.spring.chronicle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Abstract superclass of <a href="http://chronicle.software/">Chronicle</a> collection
 * {@linkplain org.springframework.beans.factory.FactoryBean} classes.
 *
 * @param <T> Type of Chronicle collection to be built
 *
 * @author <a href="mailto:dave@osframework.org">Dave Joyce</a>
 * @since 0.0.1
 */
public abstract class AbstractChronicleBuilderBean<T> extends AbstractFactoryBean<T> {

    protected final Logger slf4jLogger;

    public AbstractChronicleBuilderBean() {
        super();
        this.slf4jLogger = LoggerFactory.getLogger(this.getClass());
    }

    protected abstract <C extends AbstractBuilderConfig> C getConfig();

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
            getConfig().lockTimeOutParser = new LockTimeOutParser(lockTimeOut);
        }
    }

    /**
     * Set filesystem location to which the collection instance will persist its entries
     * off-heap. The specified value must resolve to a {@code File} that is readable and
     * writable.
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
     * Set filesystem location to which the collection instance will persist its entries
     * off-heap. The specified value must be a {@code File} that is readable and
     * writable.
     *
     * @param persistedTo off-heap entry storage filesystem location
     */
    public void setPersistedTo(File persistedTo) {
        getConfig().persistedTo = persistedTo;
    }

    /**
     * Set network addresses to which ChronicleMap instances created by this object will push entries.
     *
     * @param pushToAddresses network addresses to push to
     */
    public void setPushTo(InetSocketAddress... pushToAddresses) {
        if (0 < pushToAddresses.length) {
            getConfig().pushToAddresses = pushToAddresses;
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

    protected class AbstractBuilderConfig {

        private LockTimeOutParser lockTimeOutParser = null;
        public File persistedTo = null;
        public InetSocketAddress[] pushToAddresses = null;

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
