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
package v.a.org.springframework.session.aerospike.actors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.UUID;

import javax.inject.Inject;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.session.MapSession;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import v.a.org.springframework.session.messages.FetchSession;
import v.a.org.springframework.session.messages.SessionSnapshot;
import v.a.org.springframework.store.aerospike.AerospikeTemplate;
import v.a.org.springframework.store.aerospike.test.BaseIntegrationTest;
import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;

public class ActorsInteractionIT extends BaseIntegrationTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private ActorRef supervisorRef;

    @Inject
    private AerospikeTemplate aerospikeTemplate;

    @Test
    public void init() {
        assertThat(supervisorRef, notNullValue());
    }

    @Test
    public void save_session() throws Exception {
        String id = UUID.randomUUID().toString();
        SessionSnapshot ss = createSessionSnapshot(id);
        supervisorRef.tell(ss, null);
        

        Thread.sleep(2000);
        assertThat(aerospikeTemplate.hasKey(id), is(true));
    }
    
    @Test
    public void saveAndGet_session() throws Exception {
        String id = UUID.randomUUID().toString();
        SessionSnapshot ss = createSessionSnapshot(id);
        supervisorRef.tell(ss, null);
        
        Thread.sleep(5000);
        assertThat(aerospikeTemplate.hasKey(id), is(true));
        
        Timeout timeout = new Timeout(Duration.create(600, "seconds"));
        Future<Object> future = Patterns.ask(supervisorRef, new FetchSession(id), timeout);
        
        MapSession result = (MapSession) Await.result(future, timeout.duration());
        assertThat(result.getId(), is(id));


        
    }

    private SessionSnapshot createSessionSnapshot(final String id) {
        Long now = System.currentTimeMillis();
        final SessionSnapshot.Builder builder = new SessionSnapshot.Builder(id)
                .creationTime(now)
                .expirationTimestamp(now + 60000)
                .lastAccessedTime(now)
                .maxInactiveIntervalInSec(60)
                .updated(true)
                .addAattribute("A", "DEADBEEF")
                .addAattribute("B", "DEADBEEF");

        return builder.build();
    }

}
