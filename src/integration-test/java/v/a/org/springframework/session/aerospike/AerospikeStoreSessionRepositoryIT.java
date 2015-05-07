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
package v.a.org.springframework.session.aerospike;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import v.a.org.springframework.session.aerospike.AerospikeStoreSessionRepository.AerospikeSession;

import v.a.org.springframework.session.aerospike.config.annotation.web.http.EnableAerospikeHttpSession;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.async.AsyncClient;
import com.aerospike.client.async.AsyncClientPolicy;
import com.aerospike.client.async.IAsyncClient;
import com.aerospike.client.policy.ClientPolicy;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
public class AerospikeStoreSessionRepositoryIT {

    private final Logger log = LoggerFactory.getLogger(this.getClass());   

    @Inject
    private SessionRepository<AerospikeStoreSessionRepository.AerospikeSession> repository;

    @Test
    public void when_contextStarted_thenNoExceptions() {
        log.info("Spring context loaded. Session repository: {}", repository);
    }
        
    @Test
    public void createSession() {
        AerospikeSession s = repository.createSession();
        log.info("New session: {}", s.getId());
        assertThat(s, notNullValue());
        assertThat(s.getId(), notNullValue());
    }
    
    @Test
    public void save_then_load() {
        final AerospikeSession s = repository.createSession();
        final String sessionId = s.getId();
        s.setAttribute("A", "XYZ");
        log.info("New session to store: {}", sessionId);
        repository.save(s);
        
        final AerospikeSession loadedSession = repository.getSession(sessionId);
                
        assertThat(loadedSession, notNullValue());
        assertThat(loadedSession.getId(), is(sessionId));
        assertThat((String)loadedSession.getAttribute("A"), is("XYZ"));

    }
    
    @Test
    public void delete_notExist_thenNoErrors() {
        repository.delete("notExist");
    }
    
    @Test
    public void delete() {        
        final AerospikeSession s = repository.createSession();
        final String sessionId = s.getId();
        s.setAttribute("A", "XYZ");
        repository.save(s);

        final AerospikeSession loadedSession = repository.getSession(sessionId);                
        assertThat(loadedSession, notNullValue());
        repository.delete(sessionId);
        // should not exist in repo anymore 
        assertThat(repository.getSession(sessionId), nullValue());
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
