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

/**
 * Akka actors eco-system.
 *
 */
public abstract class ActorsEcoSystem {
    
    public final static String SUPERVISOR = "supervisor";
    
    final static String SEESION_REMOVER = "sessionRemover";
    
    final static String EXPIRED_SESSIONS_CARETAKER = "expiredSessionsCaretaker";
    
    final static String SESSION_DELETED_NOTIFIER = "sessionDeletedNotifier";
    
    final static String DELETE_SESSION_WORKER = "deleteSessionWorker";
    
    final static String SESSION_SERIALIZER = "sessionSerializer";
    
    final static String SERIALIZE_SESSION_WORKER = "serializeSessionWorker";
    
    final static String SESSION_PERSISTER = "sessionPersister";
    
    final static String SESSION_LOADER = "sessionLoader";

}