/*
 * Copyright 2015 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package us.swcraft.springframework.session.transformer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.swcraft.org.springframework.store.aerospike.test.BaseIntegrationTest;

public class DefaultSessionAttributesTransformerIT extends BaseIntegrationTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private DefaultSessionAttributesTransformer transformer;

    @Test
    public void when_contextStarted_thenNoExceptions() {
        log.info("Spring context loaded: {}", transformer);
    }

    @Test
    public void marshal_unmarshal() {
        Map<String, Object> attrs = new HashMap<>();

        attrs.put("S", "DEADBEF");
        attrs.put("UUID", UUID.randomUUID());

        byte[] binarySession = transformer.marshall(attrs);
        assertThat(binarySession, notNullValue());

        Map<String, Object> restoredAttrs = transformer.unmarshal(binarySession);
        assertThat(restoredAttrs, notNullValue());
        assertThat(restoredAttrs.get("S"), is("DEADBEF"));
        assertThat(restoredAttrs.get("UUID"), notNullValue());
    }

    @Test
    public void marshal_unmarshal_Serializable() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("S", new S());

        byte[] binarySession = transformer.marshall(attrs);
        assertThat(binarySession, notNullValue());

        Map<String, Object> restoredAttrs = transformer.unmarshal(binarySession);
        assertThat(restoredAttrs, notNullValue());
        assertThat(restoredAttrs.get("S"), notNullValue());
        assertThat(((S)restoredAttrs.get("S")).getContent(), is("DEADBEF"));
    }
    
    @Test
    public void marshal_unmarshal_not_Serializable() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("NS", new NS());

        byte[] binarySession = transformer.marshall(attrs);
        assertThat(binarySession, notNullValue());

        Map<String, Object> restoredAttrs = transformer.unmarshal(binarySession);
        assertThat(restoredAttrs, notNullValue());
        assertThat(restoredAttrs.get("NS"), nullValue());
    }
}
