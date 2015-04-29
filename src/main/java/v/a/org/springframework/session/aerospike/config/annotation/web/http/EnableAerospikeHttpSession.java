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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.async.IAsyncClient;

/**
 * Add this annotation to an {@code @Configuration} class to expose the
 * SessionRepositoryFilter as a bean named "springSessionRepositoryFilter" and
 * backed by Aerospike. In order to leverage the annotation, a single instance of each {@link IAerospikeClient} and
 * {@link IAsyncClient} must be provided. For example:
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
 * 
 *     {@literal @Bean(destroyMethod = "close")}
 *     public AsyncClient aerospikeAsyncClient() throws Exception {
 *         return new AsyncClient("localhost", 3000);
 *     }
 * 
 * }
 * </pre>
 *
 * More advanced configurations can extend {@link AerospikeHttpSessionConfiguration} instead.
 *
 * @author Vlad Aleksandrov
 */
@Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(value = { java.lang.annotation.ElementType.TYPE })
@Documented
@Import(AerospikeHttpSessionConfiguration.class)
@Configuration
public @interface EnableAerospikeHttpSession {
    int maxInactiveIntervalInSeconds() default 1800;
    String namespace() default "cache";
    String setname() default "httpsession";
}