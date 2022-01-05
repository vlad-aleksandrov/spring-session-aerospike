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
package us.swcraft.springframework.session.store.fst;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.iq80.snappy.SnappyFramedInputStream;
import org.iq80.snappy.SnappyFramedOutputStream;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.util.Pool;

import us.swcraft.springframework.session.store.SerializationException;
import us.swcraft.springframework.session.store.StoreCompression;
import us.swcraft.springframework.session.store.StoreSerializer;

/**
 * FST: fast java serialization drop in-replacement,
 *
 * @param <T>
 */
public class FastStoreSerializer<T> implements StoreSerializer<T> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Compression type. Default is {@link StoreCompression.NONE}.
     */
    private StoreCompression compressionType = StoreCompression.NONE;

    private Pool<FSTConfiguration> fstConfPool;

    public FastStoreSerializer() {
        init();
    }

    public FastStoreSerializer(final StoreCompression compressionType) {
        this.compressionType = compressionType;
        init();
    }

    private void init() {
        final int poolSize = 64;
        fstConfPool = new Pool<FSTConfiguration>(true, false, poolSize) {
            protected FSTConfiguration create() {
                FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
                return conf;
            }
        };
        // pre-initialize FSTConfiguration objects in pool
        final FSTConfiguration[] confObj = new FSTConfiguration[poolSize];
        for (int i = 0; i < poolSize; ++i) {
            confObj[i] = fstConfPool.obtain();
        }
        for (int i = 0; i < poolSize; ++i) {
            fstConfPool.free(confObj[i]);
        }
    }

    @Override
    public byte[] serialize(final T data) throws SerializationException {
        final FSTConfiguration conf = fstConfPool.obtain();
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8192);
                final OutputStream compressionOutputStream = wrapOutputStream(outputStream);
                final FSTObjectOutput output = new FSTObjectOutput(compressionOutputStream, conf);) {
            output.writeObject(data);
            output.flush();
            compressionOutputStream.flush();
            outputStream.flush();
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Serialization error: {}", e.getMessage());
            log.trace("", e);
            throw new SerializationException(data.getClass() + " serialization problem", e);
        } finally {
            conf.clearCaches();
            fstConfPool.free(conf);
        }

    }

    @Override
    public T deserialize(final byte[] serializedData, final Class<T> type) throws SerializationException {
        final FSTConfiguration conf = fstConfPool.obtain();
        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(serializedData);
                final InputStream decompressionInputStream = wrapInputStream(inputStream);
                final FSTObjectInput input = new FSTObjectInput(decompressionInputStream, conf);) {

            @SuppressWarnings("unchecked")
            final T result = (T) input.readObject();
            return result;
        } catch (Exception e) {
            log.error("Deserialization error: {}", e.getMessage());
            log.trace("", e);
            throw new SerializationException(type + " deserialization problem", e);
        } finally {
            fstConfPool.free(conf);
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

}
