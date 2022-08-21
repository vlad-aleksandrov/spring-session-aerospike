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
package us.swcraft.springframework.session.aerospike;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URL;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.session.SessionRepository;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;
import us.swcraft.org.springframework.store.aerospike.test.BaseIntegrationTest;
import us.swcraft.springframework.session.aerospike.AerospikeStoreSessionRepository.AerospikeSession;

public class AerospikeStoreSessionRepositoryIT extends BaseIntegrationTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private SessionRepository<AerospikeStoreSessionRepository.AerospikeSession> repository;

    @Test
    public void when_contextStarted_thenNoExceptions() {
        log.info("Spring context loaded. Session repository: {}", repository);
    }

    @Test
    public void logger() {
        LoggerContext loggerContext = ((ch.qos.logback.classic.Logger) log).getLoggerContext();
        URL mainURL = ConfigurationWatchListUtil.getMainWatchURL(loggerContext);
        log.info("Logback used '{}' as the configuration file.", mainURL);
    }

    @Test
    public void createSession() {
        AerospikeSession s = repository.createSession();
        log.info("New session: {}", s.getId());
        assertThat(s, notNullValue());
        assertThat(s.getId(), notNullValue());
    }

    @Test
    public void save_then_load() throws InterruptedException {
        final AerospikeSession s = repository.createSession();
        final String sessionId = s.getId();
        s.setAttribute("A", "XYZ");
        log.info("New session to store: {}", sessionId);
        repository.save(s);

        Thread.sleep(500L);

        final AerospikeSession loadedSession = repository.getSession(sessionId);

        assertThat(loadedSession, notNullValue());
        assertThat(loadedSession.getId(), is(sessionId));
        assertThat((String) loadedSession.getAttribute("A"), is("XYZ"));

    }

    @Test
    public void delete_notExist_thenNoErrors() {
        repository.delete("notExist");
    }

    @Test
    public void delete() throws InterruptedException {
        final AerospikeSession s = repository.createSession();
        final String sessionId = s.getId();
        s.setAttribute("A", "XYZ");
        repository.save(s);
        Thread.sleep(500L);
        final AerospikeSession loadedSession = repository.getSession(sessionId);
        assertThat(loadedSession, notNullValue());
        repository.delete(sessionId);
        Thread.sleep(500L);
        // should not exist in repo anymore
        assertThat(repository.getSession(sessionId), nullValue());
    }

}
