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

import cn.danielw.fop.ObjectFactory;
import cn.danielw.fop.ObjectPool;
import cn.danielw.fop.PoolConfig;
import cn.danielw.fop.Poolable;
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

    private ObjectPool<FSTConfiguration> fstConfPool;

    public FastStoreSerializer() {
        init();
    }

    public FastStoreSerializer(final StoreCompression compressionType) {
        this.compressionType = compressionType;
        init();
    }

    private void init() {
        final PoolConfig poolConfig = new PoolConfig();
        poolConfig.setPartitionSize(8);
        poolConfig.setMaxSize(8);
        poolConfig.setMinSize(8);

        final ObjectFactory<FSTConfiguration> fstConfConfactory = new ObjectFactory<FSTConfiguration>() {
            @Override
            public FSTConfiguration create() {
                return FSTConfiguration.createDefaultConfiguration();
            }

            @Override
            public void destroy(FSTConfiguration conf) {
                // clean up and release resources
                conf.clearCaches();
            }

            @Override
            public boolean validate(FSTConfiguration conf) {
                return true;
            }
        };

        fstConfPool = new ObjectPool<>(poolConfig, fstConfConfactory);

    }

    @Override
    public byte[] serialize(final T data) throws SerializationException {
        try (Poolable<FSTConfiguration> po = fstConfPool.borrowObject()) {
            final FSTConfiguration conf = po.getObject();

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
            }
        }
    }

    @Override
    public T deserialize(final byte[] serializedData, final Class<T> type) throws SerializationException {
        try (Poolable<FSTConfiguration> po = fstConfPool.borrowObject()) {
            final FSTConfiguration conf = po.getObject();
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
            fstConfPool.shutdown();
        } catch (InterruptedException e) {
            // no-op
        }        
    }

}
