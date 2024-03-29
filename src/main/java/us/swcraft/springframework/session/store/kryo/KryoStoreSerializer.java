/*
 * Copyright 2015-2022 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package us.swcraft.springframework.session.store.kryo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;

import org.iq80.snappy.SnappyFramedInputStream;
import org.iq80.snappy.SnappyFramedOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.LocaleSerializer;

import cn.danielw.fop.ObjectFactory;
import cn.danielw.fop.ObjectPool;
import cn.danielw.fop.PoolConfig;
import cn.danielw.fop.Poolable;
import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptyListSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptyMapSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptySetSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonListSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonMapSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonSetSerializer;
import de.javakaffee.kryoserializers.GregorianCalendarSerializer;
import de.javakaffee.kryoserializers.JdkProxySerializer;
import de.javakaffee.kryoserializers.KryoReflectionFactorySupport;
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import us.swcraft.springframework.session.model.MarshalledAttribute;
import us.swcraft.springframework.session.store.SerializationException;
import us.swcraft.springframework.session.store.StoreCompression;
import us.swcraft.springframework.session.store.StoreSerializer;

/**
 * Kryo serializer.
 *
 * @param <T>
 */
public class KryoStoreSerializer<T> implements StoreSerializer<T> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Compression type. Default is {@link StoreCompression.NONE}.
     */
    private StoreCompression compressionType = StoreCompression.NONE;

    private ObjectPool<Kryo> kryoPool;

    public KryoStoreSerializer() {
        init();
    }

    public KryoStoreSerializer(final StoreCompression compressionType) {
        this.compressionType = compressionType;
        init();
    }

    private void init() {

        final PoolConfig poolConfig = new PoolConfig();
        poolConfig.setPartitionSize(8);
        poolConfig.setMaxSize(8);
        poolConfig.setMinSize(8);

        final ObjectFactory<Kryo> kryoFactory = new ObjectFactory<Kryo>() {
            @Override
            public Kryo create() {
                Kryo kryo = new Kryo();
                // Configure the Kryo instance.
                kryo = new KryoReflectionFactorySupport();
                kryo.setRegistrationRequired(false);
                kryo.addDefaultSerializer(Locale.class, LocaleSerializer.class);

                kryo.register(Arrays.asList("").getClass(), new ArraysAsListSerializer());
                kryo.register(Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer());
                kryo.register(Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer());
                kryo.register(Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer());
                kryo.register(Collections.singletonList("").getClass(), new CollectionsSingletonListSerializer());
                kryo.register(Collections.singleton("").getClass(), new CollectionsSingletonSetSerializer());
                kryo.register(Collections.singletonMap("", "").getClass(), new CollectionsSingletonMapSerializer());
                kryo.register(Collections.unmodifiableMap(new HashMap<>()).getClass(),
                        new UnmodifiableCollectionsSerializer());

                kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer());
                kryo.register(InvocationHandler.class, new JdkProxySerializer());
                UnmodifiableCollectionsSerializer.registerSerializers(kryo);
                SynchronizedCollectionsSerializer.registerSerializers(kryo);

                // Register our internal types
                kryo.register(HashMap.class, 128);
                kryo.register(AbstractMap.SimpleImmutableEntry.class, 129);

                kryo.register(MarshalledAttribute.class, 256);

                return kryo;
            }

            @Override
            public void destroy(final Kryo kryo) {
                // no-op
            }

            @Override
            public boolean validate(final Kryo kryo) {
                return true;
            }
        };

        kryoPool = new ObjectPool<>(poolConfig, kryoFactory);
    }

    @Override
    public byte[] serialize(final T data) throws SerializationException {
        try (Poolable<Kryo> po = kryoPool.borrowObject()) {
            final Kryo kryo = po.getObject();

            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8192);
                    final OutputStream compressionOutputStream = wrapOutputStream(outputStream);
                    final Output output = new Output(compressionOutputStream);) {

                kryo.writeClassAndObject(output, data);
                output.flush();
                compressionOutputStream.flush();
                outputStream.flush();
                return outputStream.toByteArray();
            } catch (Exception e) {
                log.error("Serialization error: {}", e.getMessage());
                log.trace("", e);
                throw new SerializationException(data.getClass() + " serialization problem", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(final byte[] serializedData, final Class<T> type) throws SerializationException {
        try (Poolable<Kryo> po = kryoPool.borrowObject()) {
            final Kryo kryo = po.getObject();

            try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(serializedData);
                    final InputStream decompressionInputStream = wrapInputStream(inputStream);
                    final Input input = new Input(decompressionInputStream);) {

                final T result = (T) kryo.readClassAndObject(input);
                return result;
            } catch (Exception e) {
                log.error("Deserialization error: {}", e.getMessage());
                log.trace("", e);
                throw new SerializationException(type + " deserialization problem", e);
            }
        }
    }

    private OutputStream wrapOutputStream(final OutputStream os) throws IOException {
        switch (compressionType) {
            case SNAPPY:
                return new SnappyFramedOutputStream(os);
            default:
                return new BufferedOutputStream(os);
        }
    }

    private InputStream wrapInputStream(final InputStream is) throws IOException {
        switch (compressionType) {
            case SNAPPY:
                return new SnappyFramedInputStream(is, false);
            default:
                return new BufferedInputStream(is);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        try {
            kryoPool.shutdown();
        } catch (InterruptedException e) {
            // no-op
        }
    }

}
