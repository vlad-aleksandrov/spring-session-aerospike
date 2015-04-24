/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package v.a.org.springframework.session.aerospike.config.annotation.web.http;

import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.ExpiringSession;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.RedisOperationsSessionRepository;
import org.springframework.session.data.redis.SessionMessageListener;
import org.springframework.session.data.redis.config.ConfigureNotifyKeyspaceEventsAction;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.web.http.HttpSessionStrategy;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.util.ClassUtils;

/**
 * Exposes the {@link SessionRepositoryFilter} as a bean named
 * "springSessionRepositoryFilter". In order to use this a single
 * {@link RedisConnectionFactory} must be exposed as a Bean.
 *
 * @author Rob Winch
 * @since 1.0
 *
 * @see EnableAerospikeHttpSession
 */
@Configuration
@EnableScheduling
public class AerospikeHttpSessionConfiguration implements ImportAware, BeanClassLoaderAware {

	private ClassLoader beanClassLoader;

	private Integer maxInactiveIntervalInSeconds = 1800;

	private HttpSessionStrategy httpSessionStrategy;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	private ConfigureRedisAction configureRedisAction = new ConfigureNotifyKeyspaceEventsAction();

	@Bean
	public RedisMessageListenerContainer redisMessageListenerContainer(
			RedisConnectionFactory connectionFactory) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.addMessageListener(redisSessionMessageListener(),
				Arrays.asList(new PatternTopic("__keyevent@*:del"),new PatternTopic("__keyevent@*:expired")));
		return container;
	}

	@Bean
	public SessionMessageListener redisSessionMessageListener() {
		return new SessionMessageListener(eventPublisher);
	}

	@Bean
	public RedisTemplate<String,ExpiringSession> sessionRedisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, ExpiringSession> template = new RedisTemplate<String, ExpiringSession>();
		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setConnectionFactory(connectionFactory);
		return template;
	}

	@Bean
	public RedisOperationsSessionRepository sessionRepository(RedisTemplate<String, ExpiringSession> sessionRedisTemplate) {
		RedisOperationsSessionRepository sessionRepository = new RedisOperationsSessionRepository(sessionRedisTemplate);
		sessionRepository.setDefaultMaxInactiveInterval(maxInactiveIntervalInSeconds);
		return sessionRepository;
	}

	@Bean
	public <S extends ExpiringSession> SessionRepositoryFilter<? extends ExpiringSession> springSessionRepositoryFilter(SessionRepository<S> sessionRepository, ServletContext servletContext) {
		SessionRepositoryFilter<S> sessionRepositoryFilter = new SessionRepositoryFilter<S>(sessionRepository);
		sessionRepositoryFilter.setServletContext(servletContext);
		if(httpSessionStrategy != null) {
			sessionRepositoryFilter.setHttpSessionStrategy(httpSessionStrategy);
		}
		return sessionRepositoryFilter;
	}

	public void setMaxInactiveIntervalInSeconds(int maxInactiveIntervalInSeconds) {
		this.maxInactiveIntervalInSeconds = maxInactiveIntervalInSeconds;
	}

	public void setImportMetadata(AnnotationMetadata importMetadata) {

		Map<String, Object> enableAttrMap = importMetadata.getAnnotationAttributes(EnableAerospikeHttpSession.class.getName());
		AnnotationAttributes enableAttrs = AnnotationAttributes.fromMap(enableAttrMap);
		if(enableAttrs == null) {
			// search parent classes
			Class<?> currentClass = ClassUtils.resolveClassName(importMetadata.getClassName(), beanClassLoader);
			for(Class<?> classToInspect = currentClass ;classToInspect != null; classToInspect = classToInspect.getSuperclass()) {
				EnableAerospikeHttpSession enableWebSecurityAnnotation = AnnotationUtils.findAnnotation(classToInspect, EnableAerospikeHttpSession.class);
				if(enableWebSecurityAnnotation == null) {
					continue;
				}
				enableAttrMap = AnnotationUtils
						.getAnnotationAttributes(enableWebSecurityAnnotation);
				enableAttrs = AnnotationAttributes.fromMap(enableAttrMap);
			}
		}
		maxInactiveIntervalInSeconds = enableAttrs.getNumber("maxInactiveIntervalInSeconds");
	}

	@Autowired(required = false)
	public void setHttpSessionStrategy(HttpSessionStrategy httpSessionStrategy) {
		this.httpSessionStrategy = httpSessionStrategy;
	}

	@Bean
	public EnableRedisKeyspaceNotificationsInitializer enableRedisKeyspaceNotificationsInitializer(RedisConnectionFactory connectionFactory) {
		return new EnableRedisKeyspaceNotificationsInitializer(connectionFactory, configureRedisAction);
	}

	/**
	 * Ensures that Redis is configured to send keyspace notifications. This is important to ensure that expiration and
	 * deletion of sessions trigger SessionDestroyedEvents. Without the SessionDestroyedEvent resources may not get
	 * cleaned up properly. For example, the mapping of the Session to WebSocket connections may not get cleaned up.
	 */
	static class EnableRedisKeyspaceNotificationsInitializer implements InitializingBean {
		static final String CONFIG_NOTIFY_KEYSPACE_EVENTS = "notify-keyspace-events";

		private final RedisConnectionFactory connectionFactory;

		private ConfigureRedisAction configure;

		EnableRedisKeyspaceNotificationsInitializer(RedisConnectionFactory connectionFactory, ConfigureRedisAction configure) {
			this.connectionFactory = connectionFactory;
			this.configure = configure;
		}

		public void afterPropertiesSet() throws Exception {
			RedisConnection connection = connectionFactory.getConnection();
			configure.configure(connection);
		}
	}

	/**
	 * Sets the action to perform for configuring Redis.
	 *
	 * @param configureRedisAction the configureRedis to set. The default is {@link ConfigureNotifyKeyspaceEventsAction}.
	 */
	@Autowired(required = false)
	public void setConfigureRedisAction(ConfigureRedisAction configureRedisAction) {
		this.configureRedisAction = configureRedisAction;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang.ClassLoader)
	 */
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}
}
