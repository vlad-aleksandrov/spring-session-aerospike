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
import java.util.Locale;

import org.iq80.snappy.SnappyInputStream;
import org.iq80.snappy.SnappyOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import v.a.org.springframework.store.SerializationException;
import v.a.org.springframework.store.StoreSerializer;
import v.a.org.springframework.store.StoreCompression;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.LocaleSerializer;

import de.javakaffee.kryoserializers.KryoReflectionFactorySupport;

public class KryoStoreSerializer<T> implements StoreSerializer<T> {
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());    

    /**
     * Compression type. Default is {@link StoreCompression.NONE}.
     */
    private StoreCompression compressionType = StoreCompression.NONE;

    /**
     * Setup ThreadLocal of Kryo instances, so this implementation will be thread-safe.
     */
    private ThreadLocal<Kryo> kryos = new ThreadLocal<Kryo>() {
        protected Kryo initialValue() {
            // configure kryo instance, customize settings
            final Kryo kryo = new KryoReflectionFactorySupport();
            kryo.addDefaultSerializer(Locale.class, LocaleSerializer.class);
            return kryo;
        };
    };

    public KryoStoreSerializer() {
    }

    public KryoStoreSerializer(final StoreCompression compressionType) {
        this.compressionType = compressionType;
    }

    @Override
    public void cleanup() {
        kryos = null;
    }

    @Override
    public byte[] serialize(final T data) throws SerializationException {
        final Kryo kryo = kryos.get();
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
        final Kryo kryo = kryos.get();
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
