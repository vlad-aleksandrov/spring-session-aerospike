/*
 * Copyright 2016 the original author or authors.
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
package us.swcraft.springframework.session.messages;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Test;

public class SessionAttributesBinaryTest {

    @Test(expected = NullPointerException.class)
    public void constructor_null() throws IOException, ClassNotFoundException {
        new SessionAttributesBinary(null);
    }

    @Test
    public void constructor_empty() throws IOException, ClassNotFoundException {
        SessionAttributesBinary attr = new SessionAttributesBinary(new byte[0]);
        assertThat(attr.getAttributes().length, is(0));
    }

}
