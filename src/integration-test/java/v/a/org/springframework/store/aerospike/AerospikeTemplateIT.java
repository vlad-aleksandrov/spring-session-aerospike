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
package v.a.org.springframework.store.aerospike;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import v.a.org.springframework.session.aerospike.config.annotation.web.http.EnableAerospikeHttpSession;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Host;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Record;
import com.aerospike.client.async.AsyncClient;
import com.aerospike.client.async.AsyncClientPolicy;
import com.aerospike.client.async.IAsyncClient;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.query.IndexType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
public class AerospikeTemplateIT {

    private final Logger log = LoggerFactory.getLogger(this.getClass());   

    @Inject
    private AerospikeTemplate template;
    
    
    @Before
    public void prepare() {
        //template.deleteAll();
    }

    @Test
    public void when_contextStarted_thenNoExceptions() {
        log.info("Spring context loaded. Aerospike Template: {}", template);
    }
    
    @Test
    public void fetch() throws InterruptedException {
        template.createIndex("expired", "expiredIndx", IndexType.NUMERIC);
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
    public void hasKey() throws InterruptedException {
        assertThat("not exist", template.hasKey(UUID.randomUUID().toString()), is(false));
        String id = UUID.randomUUID().toString();
        Set<Bin> bins = new HashSet<>();
        bins.add(new Bin("sessionId", id));
        bins.add(new Bin("expired", Long.MAX_VALUE));        
        template.persist(id, bins);
        assertThat("exist", template.hasKey(id), is(true));
    }
    

    @Configuration
    @PropertySource(value = "classpath:/application.properties")
    @EnableAerospikeHttpSession(namespace = "cache", setname = "httpsessionIT")
    static class Config {
        
        @Inject
        private Environment env;

        @Bean(destroyMethod = "close")
        public IAerospikeClient aerospikeClient() throws Exception {
            final ClientPolicy defaultClientPolicy = new ClientPolicy();
            final IAerospikeClient client = new AerospikeClient(defaultClientPolicy, new Host(env.getProperty("aerospike.host"),
                    Integer.valueOf(env.getProperty("aerospike.port"))));
            return client;
        }

        @Bean(destroyMethod = "close")
        public IAsyncClient aerospikeAsyncClient() throws Exception {
            final AsyncClientPolicy defaultAsyncClientPolicy = new AsyncClientPolicy();
            final IAsyncClient client = new AsyncClient(defaultAsyncClientPolicy, new Host(env.getProperty("aerospike.host"),
                    Integer.valueOf(env.getProperty("aerospike.port"))));
            return client;
        }
    }

}
