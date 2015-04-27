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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import v.a.org.springframework.store.StoreCompression;

public class KryoStoreSerializerTest {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Test
    public void serializeAndDeserializeCompressionSnappy_String() throws IOException {
        String token = "Vestibulum ut consectetur orci. Nullam pulvinar dui quis scelerisque suscipit. Integer in nisl a orci imperdiet posuere.";
        KryoStoreSerializer<String> converter = new KryoStoreSerializer<>(StoreCompression.SNAPPY);

        byte[] marshalled = converter.serialize(token);
        assertThat(marshalled, notNullValue());
        assertThat(marshalled.length > 0, is(true));
        log.debug("Result size: {}", marshalled.length);
        String result = converter.deserialize(marshalled, String.class);

        assertThat(result, notNullValue());
        assertThat(result, is(token));
    }

    @Test
    public void serializeAndDeserializeCompressionSnappy_byteArray() throws IOException {
        String token = "Vestibulum ut consectetur orci. Nullam pulvinar dui quis scelerisque suscipit. Integer in nisl a orci imperdiet posuere.";
        byte[] original = token.getBytes();

        KryoStoreSerializer<byte[]> converter = new KryoStoreSerializer<>(StoreCompression.SNAPPY);

        byte[] marshalled = converter.serialize(original);
        assertThat(marshalled, notNullValue());
        assertThat(marshalled.length > 0, is(true));
        log.debug("Result size: {}", marshalled.length);

        byte[] result = converter.deserialize(marshalled, byte[].class);

        assertThat(result, notNullValue());
        assertThat(Arrays.equals(original, result), is(true));
    }

    @Test
    public void serializeAndDeserializeCompressionNone_String() throws IOException {
        String token = "Vestibulum ut consectetur orci. Nullam pulvinar dui quis scelerisque suscipit. Integer in nisl a orci imperdiet posuere.";
        KryoStoreSerializer<String> converter = new KryoStoreSerializer<>(StoreCompression.NONE);

        byte[] marshalled = converter.serialize(token);
        assertThat(marshalled, notNullValue());
        assertThat(marshalled.length > 0, is(true));
        log.debug("Result size: {}", marshalled.length);
        String result = converter.deserialize(marshalled, String.class);

        assertThat(result, notNullValue());
        assertThat(result, is(token));
    }
}
