/*
 * Copyright 2014 CyberVision, Inc.
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

package org.kaaproject.kaa.client.channel.impl.transports;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.kaaproject.kaa.client.channel.EventTransport;
import org.kaaproject.kaa.client.event.EventManager;
import org.kaaproject.kaa.common.TransportType;
import org.kaaproject.kaa.common.endpoint.gen.Event;
import org.kaaproject.kaa.common.endpoint.gen.EventSyncRequest;
import org.kaaproject.kaa.common.endpoint.gen.EventSyncResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultEventTransport extends AbstractKaaTransport implements
        EventTransport {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultEventTransport.class);
    private final Map<Integer, List<Event>> sentEvents = new HashMap<Integer, List<Event>>();
    private EventManager manager;
    private final EventComparator eventComparator = new EventComparator();

    @Override
    public void sync() {
        syncByType(TransportType.EVENT);
    }

    @Override
    public EventSyncRequest createEventRequest(Integer requestId) {
        if (manager != null) {
            EventSyncRequest request = new EventSyncRequest();

            manager.fillEventListenersSyncRequest(request);

            List<Event> events = new LinkedList<Event>();
            if (!sentEvents.isEmpty()) {
                for (Map.Entry<Integer, List<Event>> pendingEvents : sentEvents.entrySet()) {
                    LOG.debug("Have not received response for {} events sent with request id {}",
                            pendingEvents.getValue().size(), pendingEvents.getKey());
                    events.addAll(pendingEvents.getValue());
                }
                sentEvents.clear();
            }

            events.addAll(manager.getPendingEvents());

            if (!events.isEmpty()) {
                Collections.sort(events, eventComparator);
                LOG.debug("Going to send {} event{}", events.size()
                        , (events.size() == 1 ? "" : "s")); //NOSONAR
                request.setEvents(events);
                sentEvents.put(requestId, events);
            }
            return request;
        }
        return null;
    }

    @Override
    public void onEventResponse(EventSyncResponse response) {
        if (manager != null) {
            if (response.getEvents() != null && !response.getEvents().isEmpty()) {
                List<Event> events = new ArrayList<>(response.getEvents());
                Collections.sort(events, eventComparator);
                for (Event event : events) {
                    manager.onGenericEvent(event.getEventClassFQN(), event.getEventData().array(), event.getSource());
                }
                if (response.getEventListenersResponses() != null && !response.getEventListenersResponses().isEmpty()) {
                    manager.eventListenersResponseReceived(response.getEventListenersResponses());
                }
            }
        }
        LOG.info("Processed event response");
    }

    @Override
    public void setEventManager(EventManager manager) {
        this.manager = manager;
    }

    class EventComparator implements Comparator<Event>{

        @Override
        public int compare(Event e1, Event e2) {
            return e1.getSeqNum() - e2.getSeqNum();
        }

    }

    @Override
    public void onSyncResposeIdReceived(Integer requestId) {
        LOG.debug("Events sent with request id {} were accepted.", requestId);
        sentEvents.remove(requestId);
    }

}
