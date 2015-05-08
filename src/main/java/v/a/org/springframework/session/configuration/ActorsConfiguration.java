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
        "v.a.org.springframework.session.actors"
})
public class ActorsConfiguration {

    /**
     * Actor system singleton for this application.
     */
    @Bean(destroyMethod = "shutdown")
    @DependsOn("springExtension")
    public ActorSystem actorSystem() {
        ActorSystem system = ActorSystem.create("AkkaControlSessionsProcessing", akkaConfiguration());
        return system;
    }
    
    @Bean
    @Inject
    public ActorRef supervisorRef(ActorSystem actorSystem, SpringExtension ext) {
        return actorSystem.actorOf(ext.props("supervisor"));
    }
    
    
    
    /**
     * Reads configuration from {@code classpath:/application.conf} file
     */
    private Config akkaConfiguration() {
        return ConfigFactory.load();
    }
}
