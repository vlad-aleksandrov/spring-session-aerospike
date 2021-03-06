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
package us.swcraft.springframework.session.aerospike.actors;

/**
 * Akka actors eco-system.
 *
 */
public abstract class ActorsEcoSystem {

    public final static String EXPIRED_SESSIONS_CARETAKER = "expiredSessionsCaretaker";

    public final static String SEESION_REMOVER = "sessionRemover";
    final static String DELETE_SESSION_WORKER = "deleteSessionWorker";

    public final static String SESSION_DELETED_NOTIFIER = "sessionDeletedNotifier";

    public final static String SESSION_SERIALIZER = "sessionSerializer";
    final static String SERIALIZE_SESSION_WORKER = "serializeSessionWorker";
    
    public final static String ATTRIBUTE_SERIALIZER = "attributeSerializer";
    final static String SERIALIZE_ATTRIBUTE_WORKER = "serializeAttributeWorker";

    public final static String SESSION_PERSISTER = "sessionPersister";

    public final static String SESSION_FETCHER = "sessionFetcher";

    public final static String INDICES_CREATOR = "indicesCreator";

}
