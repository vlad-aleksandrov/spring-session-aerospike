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
package v.a.org.springframework.store.aerospike;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import v.a.org.springframework.session.messages.SessionDeletedNotification;

public class SessionDeletedNotificationTest {

    @Test
    public void serialize_java() throws IOException, ClassNotFoundException {
        SessionDeletedNotification originalNotification = new SessionDeletedNotification("a");
        
        ByteArrayOutputStream  buffer = new ByteArrayOutputStream();
        new ObjectOutputStream(buffer).writeObject(originalNotification);
        SessionDeletedNotification serializedNotification = (SessionDeletedNotification) new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray ())).readObject();

        assertThat(serializedNotification.getId(), is(originalNotification.getId()));
    }

    
}
