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
package v.a.org.springframework.session.configuration;

import static v.a.org.springframework.session.aerospike.actors.ActorsEcoSystem.*;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import v.a.org.springframework.session.support.SpringExtension;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@Configuration
@ComponentScan(basePackages = {
        "v.a.org.springframework.session.support",
        "v.a.org.springframework.session.aerospike.actors"
})
public class ActorsConfiguration {

    /**
     * Actor system singleton for this application.
     */
    @Bean(destroyMethod = "shutdown")
    @DependsOn("springExtension")
    public ActorSystem actorSystem() {
        ActorSystem actorSystem = ActorSystem.create("ManageSessions", akkaConfiguration());
        return actorSystem;
    }

    /**
     * Session remover actor reference.
     * @param actorSystem
     * @param ext
     * @return
     */
    @Bean
    @Inject
    public ActorRef removerRef(ActorSystem actorSystem, SpringExtension ext) {
        return actorSystem.actorOf(ext.props(SEESION_REMOVER), SEESION_REMOVER);
    }
    
    /**
     * Expired sessions caretaker actor reference.
     * @param actorSystem
     * @param ext
     * @return
     */
    @Bean
    @Inject
    public ActorRef expiredSessionsCaretakerRef(ActorSystem actorSystem, SpringExtension ext) {
        return actorSystem.actorOf(ext.props(EXPIRED_SESSIONS_CARETAKER), EXPIRED_SESSIONS_CARETAKER);
    }
    
    /**
     * Session deleted notifier actor reference.
     * @param actorSystem
     * @param ext
     * @return
     */
    @Bean
    @Inject
    public ActorRef notifierRef(ActorSystem actorSystem, SpringExtension ext) {
        return actorSystem.actorOf(ext.props(SESSION_DELETED_NOTIFIER), SESSION_DELETED_NOTIFIER);
    }
    
    /**
     * Session serializer actor reference.
     * @param actorSystem
     * @param ext
     * @return
     */
    @Bean
    @Inject
    public ActorRef serializerRef(ActorSystem actorSystem, SpringExtension ext) {
        return actorSystem.actorOf(ext.props(SESSION_SERIALIZER), SESSION_SERIALIZER);
    }
    
    /**
     * Session attribute serializer actor reference.
     * @param actorSystem
     * @param ext
     * @return
     */
    @Bean
    @Inject
    public ActorRef attributeSerializerRef(ActorSystem actorSystem, SpringExtension ext) {
        return actorSystem.actorOf(ext.props(ATTRIBUTE_SERIALIZER), ATTRIBUTE_SERIALIZER);
    }
    

    /**
     * Reads configuration from {@code classpath:/application.conf} file
     */
    private Config akkaConfiguration() {
        return ConfigFactory.load();
    }
}
