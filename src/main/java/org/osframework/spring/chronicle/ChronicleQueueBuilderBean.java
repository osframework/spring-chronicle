/*
   Copyright 2016 OSFramework Project

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.osframework.spring.chronicle;

import net.openhft.chronicle.Chronicle;
import net.openhft.chronicle.ChronicleQueueBuilder;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.io.File;

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

    private final ChronicleQueueBuilderConfig config;

    private ChronicleQueueBuilder builder = null;

    public ChronicleQueueBuilderBean() {
        super();
        this.config = new ChronicleQueueBuilderConfig();
    }

    public void setChronicleType(String chronicleType) {
        config.builderType = ChronicleQueueBuilderType.valueOf(chronicleType);
    }

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
        switch (config.builderType) {
            case INDEXED:
                builder = ChronicleQueueBuilder.indexed((File)null);
                break;
            case VANILLA:
                builder = ChronicleQueueBuilder.vanilla((File)null);
                break;
            default:
                throw new IllegalStateException("Unknown Chronicle type: " + config.builderType);
        }

        return builder.build();
    }

    @Override
    protected void destroyInstance(Chronicle instance) throws Exception {
        instance.close();
        super.destroyInstance(instance);
    }

    enum ChronicleQueueBuilderType {
        INDEXED,
        VANILLA;
    }

    final class ChronicleQueueBuilderConfig {

        private ChronicleQueueBuilderType builderType;

    }

}
