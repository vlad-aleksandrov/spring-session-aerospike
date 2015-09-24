/*
 * Copyright 2015 the original author or authors.
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
package v.a.org.springframework.store.kryo;

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

import org.iq80.snappy.SnappyInputStream;
import org.iq80.snappy.SnappyOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import v.a.org.springframework.session.messages.SerializedAttribute;
import v.a.org.springframework.store.SerializationException;
import v.a.org.springframework.store.StoreCompression;
import v.a.org.springframework.store.StoreSerializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.LocaleSerializer;

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
import de.javakaffee.kryoserializers.guava.ImmutableListSerializer;

/**
 * Kryo serializer.
 * <p/>
 * <strong>Not thread safe!</strong>
 *
 * @param <T>
 */
public class KryoStoreSerializer<T> implements StoreSerializer<T> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Compression type. Default is {@link StoreCompression.NONE}.
     */
    private StoreCompression compressionType = StoreCompression.NONE;

    private Kryo kryo;

    public KryoStoreSerializer() {
        init();
    }

    public KryoStoreSerializer(final StoreCompression compressionType) {
        this.compressionType = compressionType;
        init();
    }

    private void init() {
        kryo = new KryoReflectionFactorySupport();
        kryo.addDefaultSerializer(Locale.class, LocaleSerializer.class);

        kryo.register(Arrays.asList("").getClass(), new ArraysAsListSerializer());
        kryo.register(Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer());
        kryo.register(Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer());
        kryo.register(Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer());
        kryo.register(Collections.singletonList("").getClass(), new CollectionsSingletonListSerializer());
        kryo.register(Collections.singleton("").getClass(), new CollectionsSingletonSetSerializer());
        kryo.register(Collections.singletonMap("", "").getClass(), new CollectionsSingletonMapSerializer());
        kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer());
        kryo.register(InvocationHandler.class, new JdkProxySerializer());
        UnmodifiableCollectionsSerializer.registerSerializers(kryo);
        SynchronizedCollectionsSerializer.registerSerializers(kryo);

        // guava ImmutableList
        ImmutableListSerializer.registerSerializers(kryo);

        // Register our types
        kryo.register(SerializedAttribute.class, 128);
        kryo.register(HashMap.class, 129);
        kryo.register(AbstractMap.SimpleImmutableEntry.class, 130);

    }

    @Override
    public byte[] serialize(final T data) throws SerializationException {
        try (
                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8192);
                final OutputStream compressionOutputStream = wrapOutputStream(outputStream);
                final Output output = new Output(compressionOutputStream);) {
            kryo.writeObject(output, data);
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

    @Override
    public T deserialize(final byte[] serializedData, final Class<T> type) throws SerializationException {
        try (
                final ByteArrayInputStream inputStream = new ByteArrayInputStream(serializedData);
                final InputStream decompressionInputStream = wrapInputStream(inputStream);
                final Input input = new Input(decompressionInputStream);) {

            final T result = kryo.readObject(input, type);
            return result;
        } catch (Exception e) {
            log.error("Deserialization error: {}", e.getMessage());
            log.trace("", e);
            throw new SerializationException(type + " deserialization problem", e);
        }

    }

    private OutputStream wrapOutputStream(final OutputStream os) throws IOException {
        switch (compressionType) {
            case SNAPPY:
                return new SnappyOutputStream(os);
            default:
                return new BufferedOutputStream(os);
        }
    }

    private InputStream wrapInputStream(final InputStream is) throws IOException {
        switch (compressionType) {
            case SNAPPY:
                return new SnappyInputStream(is);
            default:
                return new BufferedInputStream(is);
        }
    }

}
