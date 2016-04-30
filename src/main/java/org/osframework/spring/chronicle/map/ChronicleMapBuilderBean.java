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
package org.osframework.spring.chronicle.map;

import net.openhft.chronicle.map.*;
import net.openhft.lang.io.serialization.BytesMarshaller;
import org.osframework.spring.chronicle.AbstractChronicleCollectionBuilderBean;
import org.osframework.spring.chronicle.InetSocketAddressEditor;

import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * Adapter implementation of {@code FactoryBean} interface to support creation of a
 * {@code ChronicleMap} object through a {@code ChronicleMapBuilder} instance. This class
 * adapts the <em>fluent API</em> builder methods of {@code ChronicleMapBuilder} to the
 * standard JavaBean API mutator style, for declarative configuration of the details of the
 * created map in a Spring BeanFactory.
 * <p>On initialization of the BeanFactory, an instance of this class produces a singleton
 * {@code ChronicleMapBuilder} object, which is thread safe. The underlying singleton
 * builder can be safely used to create both singleton and prototype scope
 * {@code ChronicleMap} objects.</p>
 *
 * @param <K> Key class of ChronicleMap to be built
 * @param <V> Value class of ChronicleMap to be built
 *
 * @author <a href="mailto:dave@osframework.org">Dave Joyce</a>
 * @since 0.0.1
 * @see net.openhft.chronicle.map.ChronicleMap
 * @see net.openhft.chronicle.map.ChronicleMapBuilder
 */
public class ChronicleMapBuilderBean<K, V> extends AbstractChronicleCollectionBuilderBean<K, ChronicleMap<K, V>> {

    private final ChronicleMapBuilderConfig config;

    private ChronicleMapBuilder<K, V> builder = null;

    public ChronicleMapBuilderBean() {
        super();
        config = new ChronicleMapBuilderConfig();
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
     * Set network addresses to which ChronicleMap instances created by this object will push entries.
     *
     * @param pushToAddresses network addresses to push to
     */
    public final void setPushTo(InetSocketAddress... pushToAddresses) {
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
    public final void setPushTo(String... pushToAddresses) {
        InetSocketAddress[] converted = new InetSocketAddress[pushToAddresses.length];
        for (int i = 0; i < pushToAddresses.length; i++) {
            InetSocketAddressEditor editor = new InetSocketAddressEditor();
            editor.setAsText(pushToAddresses[i]);
            converted[i] = (InetSocketAddress)editor.getValue();
        }
        setPushTo(converted);
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
     */
    protected ChronicleMapBuilderConfig getConfig() {
        return config;
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
        if (config.isLockTimeOutSet()) {
            builder.lockTimeOut(config.getLockTimeOutAmount(), config.getLockTimeOutUnit());
            slf4jLogger.debug("Map query operation lock timeout set to {} {}",
                              config.getLockTimeOutAmount(),
                              config.getLockTimeOutUnit());
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
    final class ChronicleMapBuilderConfig extends AbstractBuilderConfig {

        private Class<V> valueClass;
        private BytesMarshaller<? super V> valueMarshaller;
        private Double averageValueSize = null;
        private V sampleValue = null;

        private Alignment alignment = null;

        private Boolean putReturnsNull, removeReturnsNull;

        private InetSocketAddress[] pushToAddresses = null;

        private MapEventListener<K, V> eventListener = null;
        private BytesMapEventListener bytesEventListener = null;

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
