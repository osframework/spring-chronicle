package org.osframework.spring.chronicle;

import net.openhft.chronicle.Chronicle;
import net.openhft.chronicle.ChronicleQueueBuilder;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Adapter implementation of {@code FactoryBean} interface to support creation of a
 * {@code Chronicle} object through a {@code ChronicleQueueBuilder} instance. This class
 * adapts the <em>fluent API</em> builder methods of {@code ChronicleQueueBuilder} to the
 * standard JavaBean API mutator style, for declarative configuration of the details of the
 * created map in a Spring BeanFactory.
 * <p>On initialization of the BeanFactory, an instance of this class produces a singleton
 * {@code ChronicleQueueBuilder} object, which is thread safe. The underlying singleton
 * builder can be safely used to create both singleton and prototype scope
 * {@code Chronicle} objects.</p>
 *
 * @author <a href="mailto:dave@osframework.org">Dave Joyce</a>
 * @since 0.0.1
 * @see net.openhft.chronicle.Chronicle
 * @see net.openhft.chronicle.ChronicleQueueBuilder
 */
public class ChronicleQueueBuilderBean extends AbstractFactoryBean<Chronicle> {

    @Override
    public Class<?> getObjectType() {
        return Chronicle.class;
    }

    /**
     * {@inheritDoc}
     * <p>This method implementation constructs and configures a
     * {@linkplain ChronicleQueueBuilder} singleton, from which the
     * {@code Chronicle} object is produced.</p>
     *
     * @return constructed, configured {@code Chronicle} object
     * @throws Exception if queue construction fails for any reason
     */
    @Override
    protected Chronicle createInstance() throws Exception {
        return null;
    }

    @Override
    protected void destroyInstance(Chronicle instance) throws Exception {
        instance.close();
        super.destroyInstance(instance);
    }

}
