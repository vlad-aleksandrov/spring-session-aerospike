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
package us.swcraft.org.springframework.store.aerospike.test;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import us.swcraft.springframework.session.aerospike.config.annotation.web.http.EnableAerospikeHttpSession;
import us.swcraft.springframework.session.store.StoreCompression;
import us.swcraft.springframework.session.store.StoreSerializationType;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Host;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.ClientPolicy;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@WebAppConfiguration
public abstract class BaseIntegrationTest {

    @Test
    public void when_contextStarted_thenNoExceptions() {
    }

    @Configuration
    @PropertySource(value = "classpath:/application.properties")
    @EnableAerospikeHttpSession(maxInactiveIntervalInSeconds = 600, namespace = "#{'${aerospike.namespace}'}", setname = "${aerospike.setname}", serializationType = StoreSerializationType.FST, compression = StoreCompression.SNAPPY)
    static class Config {

        @Inject
        private Environment env;

        @Bean(destroyMethod = "close")
        public IAerospikeClient aerospikeClient() {
            final ClientPolicy defaultClientPolicy = new ClientPolicy();
            final IAerospikeClient client = new AerospikeClient(defaultClientPolicy,
                    new Host(env.getRequiredProperty("aerospike.host"),
                            env.getRequiredProperty("aerospike.port", Integer.class)));
            return client;
        }

    }

}
