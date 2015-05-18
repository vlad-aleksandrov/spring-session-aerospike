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

package v.a.org.springframework.session.aerospike.config.annotation.web.http;

import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.async.IAsyncClient;

/**
 * @author Vlad Aleksandrov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
public class AerospikeHttpSessionConfigurationTests {

    @Test
    public void when_contextLoaded_then_noErrors() {
    }

    @EnableAerospikeHttpSession(maxInactiveIntervalInSeconds = 600, namespace = "mockcache", setname = "mockhttpsession")
    @Configuration
    static class Config {
       

        @Bean(destroyMethod = "close")
        public IAerospikeClient aerospikeClient() {
            return mock(IAerospikeClient.class);
        }
        
        @Bean(destroyMethod = "close")
        public IAsyncClient asyncAerospikeClient() {
            return mock(IAsyncClient.class);
        }
    }
}
