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
package us.swcraft.springframework.session.aerospike.config.annotation.web.http;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.Session;

import com.aerospike.client.IAerospikeClient;

import us.swcraft.springframework.session.store.StoreCompression;
import us.swcraft.springframework.session.store.StoreSerializationType;

/**
 * Add this annotation to an {@code @Configuration} class to expose the
 * SessionRepositoryFilter as a bean named "springSessionRepositoryFilter" and
 * backed by Aerospike. In order to leverage the annotation, a single instance
 * of each {@link IAerospikeClient} must be provided. For example:
 *
 * <pre>
 * {@literal @Configuration}
 * {@literal @EnableAerospikeHttpSession}
 * public class AerospikeHttpSessionConfig {
 *     
 *     {@literal @Bean(destroyMethod = "close")}
 *     public AerospikeClient aerospikeClient() throws Exception {
 *         return new AerospikeClient("localhost", 3000);
 *     }
 * }
 * </pre>
 *
 * More advanced configurations can extend
 * {@link AerospikeHttpSessionConfiguration} instead.
 *
 * @author Vlad Aleksandrov
 */
@Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(value = { java.lang.annotation.ElementType.TYPE })
@Documented
@Import(AerospikeHttpSessionConfiguration.class)
@Configuration(proxyBeanMethods = false)
public @interface EnableAerospikeHttpSession {

    /**
     * Sets the maximum inactive interval in seconds between requests before
     * newly created sessions will be invalidated. A negative time indicates
     * that the session will never timeout. The default is 1800 (30 minutes).
     *
     * @return the number of seconds that the {@link Session} should be kept
     *         alive between client requests.
     */
    int maxInactiveIntervalInSeconds() default 1800;

    /**
     * Aerospike namespace for session data.
     * 
     * @return namespace name
     */
    String namespace() default "cache";

    /**
     * Aerospike set name for session data.
     * 
     * @return set name
     */
    String setname() default "httpsession";
    
    /**
     * Store serialization type. 
     * @return serialization type
     */
    StoreSerializationType serializationType() default StoreSerializationType.FST;  
    
    /**
     * Store compression type. 
     * @return compression type
     */
    StoreCompression compression() default StoreCompression.NONE;  
}