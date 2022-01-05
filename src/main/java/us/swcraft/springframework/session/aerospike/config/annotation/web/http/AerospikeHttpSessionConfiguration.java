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

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.session.ExpiringSession;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.HttpSessionStrategy;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.util.ClassUtils;

import com.aerospike.client.IAerospikeClient;

import us.swcraft.springframework.session.model.MarshalledAttribute;
import us.swcraft.springframework.session.model.StoreMetadata;
import us.swcraft.springframework.session.store.StoreCompression;
import us.swcraft.springframework.session.store.StoreSerializationType;
import us.swcraft.springframework.session.store.StoreSerializer;
import us.swcraft.springframework.session.store.aerospike.AerospikeTemplate;
import us.swcraft.springframework.session.store.fst.FastStoreSerializer;
import us.swcraft.springframework.session.store.kryo.KryoStoreSerializer;

/**
 * Exposes the {@link SessionRepositoryFilter} as a bean named
 * "springSessionRepositoryFilter".
 *
 * @author Vlad Aleksandrov
 *
 * @see EnableAerospikeHttpSession
 */
@Configuration
@EnableScheduling
@EnableAsync
@ComponentScan("us.swcraft.springframework.session")
public class AerospikeHttpSessionConfiguration implements ImportAware, BeanClassLoaderAware {

    private ClassLoader beanClassLoader;

    /**
     * Default max inactivity interval.
     */
    private Integer maxInactiveIntervalInSeconds = 1800;
    /**
     * Default Aerospike namespace is <code>cache</code>.
     */
    private String namespace = "cache";
    /**
     * Default Aerospike logical set name is <code>httpsession</code>.
     */
    private String setname = "httpsession";

    /**
     * Store serialization type.
     */
    private StoreSerializationType serializationType = StoreSerializationType.FST;

    /**
     * Store compression type.
     */
    private StoreCompression compression = StoreCompression.NONE;

    private HttpSessionStrategy httpSessionStrategy;
    
    
    @Bean("ssa-taskExecutor")
    public Executor taskExecutor() {
      final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(4);
      executor.setMaxPoolSize(4);
      executor.setQueueCapacity(0);
      executor.setDaemon(true);
      executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
      executor.setThreadNamePrefix("sessionStore-");
      executor.initialize();
      return executor;
    }

    @Bean(name="ssa-sessionAerospikeTemplate", initMethod = "init")
    @Inject
    public AerospikeTemplate sessionAerospikeTemplate(final IAerospikeClient aerospikeClient) {
        final AerospikeTemplate template = new AerospikeTemplate();
        template.setAerospikeClient(aerospikeClient);
        template.setNamespace(namespace);
        template.setSetname(setname);
        return template;
    }

    @Bean(name="ssa-storeMetadata")
    public StoreMetadata storeMetadata() {
        final StoreMetadata storeMetadata = new StoreMetadata();
        storeMetadata.setMaxInactiveIntervalInSeconds(this.maxInactiveIntervalInSeconds);
        storeMetadata.setNamespace(this.namespace);
        storeMetadata.setSetname(this.setname);
        storeMetadata.setSerializationType(serializationType);
        storeMetadata.setCompression(compression);
        return storeMetadata;
    }
    
    /**
     * Single attribute serializer/deserializer.
     * 
     * @return
     */
    @Bean("ssa-attributeSerializer")
    public StoreSerializer<Serializable> attributeSerializer() {
        if (serializationType == StoreSerializationType.FST) {
            switch (compression) {
                case NONE:
                    return new FastStoreSerializer<Serializable>();
                case SNAPPY:
                    return new FastStoreSerializer<Serializable>(StoreCompression.SNAPPY);
                default:
                    throw new RuntimeException("Unsupported compression " + compression);
            }
        }
        if (serializationType == StoreSerializationType.KRYO) {
            switch (compression) {
                case NONE:
                    return new KryoStoreSerializer<Serializable>();
                case SNAPPY:
                    return new KryoStoreSerializer<Serializable>(StoreCompression.SNAPPY);
                default:
                    throw new RuntimeException("Unsupported compression " + compression);
            }
        }
        throw new RuntimeException("Unsupported serializer " + serializationType);
    }
    
    /**
     * Marshalled attributes serializer/deserializer.
     * 
     * @return
     */
    @Bean("ssa-marshalledAttrobutesSerializer")
    public StoreSerializer<Map<String, MarshalledAttribute>> marshalledAttributesSerializer() {
        if (serializationType == StoreSerializationType.FST) {
            return new FastStoreSerializer<Map<String, MarshalledAttribute>>();
        }
        if (serializationType == StoreSerializationType.KRYO) {
            return new KryoStoreSerializer<Map<String, MarshalledAttribute>>();            
        }
        throw new RuntimeException("Unsupported serializer " + serializationType);
    }       

    @Bean
    public <S extends ExpiringSession> SessionRepositoryFilter<? extends ExpiringSession> springSessionRepositoryFilter(
            SessionRepository<S> sessionRepository, ServletContext servletContext) {
        final SessionRepositoryFilter<S> sessionRepositoryFilter = new SessionRepositoryFilter<S>(sessionRepository);
        sessionRepositoryFilter.setServletContext(servletContext);
        if (httpSessionStrategy != null) {
            sessionRepositoryFilter.setHttpSessionStrategy(httpSessionStrategy);
        }
        
        return sessionRepositoryFilter;
    }

    public void setMaxInactiveIntervalInSeconds(int maxInactiveIntervalInSeconds) {
        this.maxInactiveIntervalInSeconds = maxInactiveIntervalInSeconds;
    }

    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Map<String, Object> enableAttrMap = importMetadata
                .getAnnotationAttributes(EnableAerospikeHttpSession.class.getName());
        AnnotationAttributes enableAttrs = AnnotationAttributes.fromMap(enableAttrMap);
        if (enableAttrs == null) {
            // search parent classes
            Class<?> currentClass = ClassUtils.resolveClassName(importMetadata.getClassName(), beanClassLoader);
            for (Class<?> classToInspect = currentClass; classToInspect != null; classToInspect = classToInspect
                    .getSuperclass()) {
                EnableAerospikeHttpSession enableWebSecurityAnnotation = AnnotationUtils.findAnnotation(classToInspect,
                        EnableAerospikeHttpSession.class);
                if (enableWebSecurityAnnotation == null) {
                    continue;
                }
                enableAttrMap = AnnotationUtils.getAnnotationAttributes(enableWebSecurityAnnotation);
                enableAttrs = AnnotationAttributes.fromMap(enableAttrMap);
            }
        }
        maxInactiveIntervalInSeconds = enableAttrs.getNumber("maxInactiveIntervalInSeconds");
        namespace = enableAttrs.getString("namespace");
        setname = enableAttrs.getString("setname");
        serializationType = enableAttrs.getEnum("serializationType");
        compression = enableAttrs.getEnum("compression");
    }

    @Autowired(required = false)
    public void setHttpSessionStrategy(HttpSessionStrategy httpSessionStrategy) {
        this.httpSessionStrategy = httpSessionStrategy;
    }

    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }
}
