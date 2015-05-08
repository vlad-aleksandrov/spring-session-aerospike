/*
 * Copyright 2002-2015 the original author or authors.
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
package v.a.org.springframework.session.aerospike;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.session.ExpiringSession;
import org.springframework.session.MapSession;
import org.springframework.test.util.ReflectionTestUtils;

import v.a.org.springframework.session.aerospike.AerospikeStoreSessionRepository.AerospikeSession;
import v.a.org.springframework.store.aerospike.AerospikeOperations;
import akka.actor.ActorRef;

import com.aerospike.client.Bin;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({ "unchecked", "rawtypes" })
public class AerospikeStoreSessionRepositoryTests {

    @Mock
    AerospikeOperations aerospikeOperations;
    
    @Mock
    ActorRef supervisorRef;

    @Captor
    ArgumentCaptor<String> capturedSessionId;
    
    @Captor
    ArgumentCaptor<Set<Bin>> capturedBinsToSave;

    private AerospikeStoreSessionRepository aerospikeRepository;

    @Before
    public void setup() {
        this.aerospikeRepository = new AerospikeStoreSessionRepository(aerospikeOperations, supervisorRef);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorNullConnectionFactory() {
        new AerospikeStoreSessionRepository((AerospikeOperations) null, supervisorRef);
    }

    @Test
    public void constructorConnectionFactory() {
        aerospikeRepository = new AerospikeStoreSessionRepository(aerospikeOperations, supervisorRef);
        AerospikeSession session = aerospikeRepository.createSession();
        session.setAttribute("K", "V");
        aerospikeRepository.save(session);
    }
    
    @Test
    public void createSessionDefaultMaxInactiveInterval() throws Exception {
        ExpiringSession session = aerospikeRepository.createSession();
        assertThat(session.getMaxInactiveIntervalInSeconds(), is(new MapSession().getMaxInactiveIntervalInSeconds()));
    }
    
    @Test
    public void createSessionCustomMaxInactiveInterval() throws Exception {
        int interval = 1;
        aerospikeRepository.setDefaultMaxInactiveInterval(interval);
        ExpiringSession session = aerospikeRepository.createSession();
        assertThat(session.getMaxInactiveIntervalInSeconds(), is(interval));
    }

    @Test
    public void saveNewSession_withAttributes() {
        AerospikeSession session = aerospikeRepository.createSession();
        session.setAttribute("A", "B");
        session.setAttribute("C", "D");
        aerospikeRepository.save(session);        
        verify(aerospikeOperations).persist(capturedSessionId.capture(), capturedBinsToSave.capture());
        
        assertThat(capturedSessionId.getValue(), is(session.getId()));
        // save 6 bins: id, created, max inactive, last access, expired and serialized attributes
        assertThat(capturedBinsToSave.getValue().size(), is(6));
    }
    
    @Test
    public void saveNewSession_noAttributes() {
        AerospikeSession session = aerospikeRepository.createSession();
        aerospikeRepository.save(session);        
        verify(aerospikeOperations).persist(capturedSessionId.capture(), capturedBinsToSave.capture());
        
        assertThat(capturedSessionId.getValue(), is(session.getId()));
        // save 5 bins: id, created, max inactive, last access, expired
        assertThat(capturedBinsToSave.getValue().size(), is(5));
    }
    
    @Test
    public void saveLastAccessChanged() {
        AerospikeSession session = aerospikeRepository.createSession();
        session.setLastAccessedTime(12345678L);
        aerospikeRepository.save(session);  
        
        verify(aerospikeOperations).persist(capturedSessionId.capture(), capturedBinsToSave.capture());
        
        Set<Bin> binsToSave = capturedBinsToSave.getValue();
        for (Bin bin : binsToSave) {
            if (bin.name.equals(AerospikeStoreSessionRepository.LAST_ACCESSED_BIN)) {
                assertThat(bin.value.toLong(), is(12345678L));
            }
        }
    }
    
    @Test
    public void session_updatedExpiration() {
        AerospikeSession session = aerospikeRepository.createSession();
        session.setLastAccessedTime(0L);
        // default max inactive is 1800 sec
        assertThat(session.getExpirationTimestamp(), is(TimeUnit.SECONDS.toMillis(1800)));
    }
    
    @Test
    public void session_updatedAfterAttrubuteAdded() {
        AerospikeSession session = aerospikeRepository.createSession();
        session.setAttribute("A", "B");
        assertThat(session.isUpdated(), is(true));
        // reset 'updated' flag to simulate fresh loaded session
        ReflectionTestUtils.setField(session, "updated", false);
        // set the same value again, session should not be updated
        session.setAttribute("A", "B");
        assertThat(session.isUpdated(), is(false));
        // set updated attribute value
        session.setAttribute("A", "C");
        assertThat(session.isUpdated(), is(true));
    }
    
    @Test
    public void session_updatedAfterAttrubuteDeleted() {
        AerospikeSession session = aerospikeRepository.createSession();
        session.setAttribute("A", "B");
        // reset 'updated' flag to simulate fresh loaded session
        ReflectionTestUtils.setField(session, "updated", false);
        session.removeAttribute("notExist");
        // still not updated, because attr does not exist in session
        assertThat(session.isUpdated(), is(false));
        session.removeAttribute("A");
        assertThat(session.isUpdated(), is(true));
        assertThat(session.getAttribute("A"), nullValue());
    }
    
    @Test
    public void delete_notExist() {
        String id = "RandomSessionId";
        
        when(aerospikeOperations.hasKey(id)).thenReturn(false);
        
        aerospikeRepository.delete(id);
        verify(aerospikeOperations, times(1)).hasKey(id);
        verify(aerospikeOperations, times(0)).delete(id);
    }
    
    @Test
    public void delete_exist() {
        String id = "RandomSessionId";
        
        when(aerospikeOperations.hasKey(id)).thenReturn(true);
        
        aerospikeRepository.delete(id);
        verify(aerospikeOperations, times(1)).hasKey(id);
        verify(aerospikeOperations, times(1)).delete(id);
    }


}