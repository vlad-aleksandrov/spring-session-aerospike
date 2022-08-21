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
package us.swcraft.org.springframework.store.aerospike;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import com.aerospike.client.Bin;
import com.aerospike.client.Record;
import com.aerospike.client.query.IndexType;

import us.swcraft.org.springframework.store.aerospike.test.BaseIntegrationTest;
import us.swcraft.springframework.session.store.aerospike.AerospikeTemplate;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public class AerospikeTemplateIT extends BaseIntegrationTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private AerospikeTemplate template;

    @BeforeEach
    public void prepare() {
        template.deleteAll();
    }

    @Test
    public void when_contextStarted_thenNoExceptions() {
        log.info("Spring context loaded. Aerospike Template: {}", template);
    }

    @Test
    public void fetch() {
        template.createIndex("expired", "expiredIndxIT", IndexType.NUMERIC);
        String id = UUID.randomUUID().toString();
        Set<Bin> bins = new HashSet<>();
        bins.add(new Bin("sessionId", id));
        bins.add(new Bin("expired", 10000));
        template.persist(id, bins);
        Record result = template.fetch(id);
        assertThat(result, notNullValue());
        assertThat(result.getString("sessionId"), is(id));
        assertThat(result.getLong("expired"), is(10000L));
    }

    @Test
    public void createIndexAndQueryRange() {
        template.createIndex("expired", "expiredIndx", IndexType.NUMERIC);
        String id = UUID.randomUUID().toString();
        Set<Bin> bins = new HashSet<>();
        bins.add(new Bin("sessionId", id));
        bins.add(new Bin("expired", 1000));
        template.persist(id, bins);
        Set<String> result = template.fetchRange("sessionId", "expired", 999, 1001);
        assertThat(result.size(), is(1));
        for (String key : result) {
            assertThat(key, is(id));
        }
    }

    @Test
    public void hasKey() {
        assertThat("not exist", template.hasKey(UUID.randomUUID().toString()), is(false));
        String id = UUID.randomUUID().toString();
        Set<Bin> bins = new HashSet<>();
        bins.add(new Bin("sessionId", id));
        bins.add(new Bin("expired", Long.MAX_VALUE));
        template.persist(id, bins);
        assertThat("exist", template.hasKey(id), is(true));
    }

}
