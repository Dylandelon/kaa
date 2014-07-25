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

#ifndef INOTIFICATIONTRANSPORT_HPP_
#define INOTIFICATIONTRANSPORT_HPP_

#include <list>
#include <boost/shared_ptr.hpp>

#include "kaa/gen/EndpointGen.hpp"

namespace kaa {

typedef std::list<SubscriptionCommand>             SubscriptionCommands;
typedef boost::shared_ptr<NotificationSyncRequest> NotificationSyncRequestPtr;

class INotificationManager;
class INotificationProcessor;

/**
 * Updates the Notification manager state.
 */
class INotificationTransport {
public:

    /**
     * Creates a new Notification request.
     *
     * @return new Notification request.
     * @see NotificationSyncRequest
     *
     */
    virtual NotificationSyncRequestPtr createNotificationRequest() = 0;

    /**
     * Updates the state of the Notification manager according to the given response.
     *
     * @param response the response from the server.
     * @see NotificationSyncResponse
     *
     */
    virtual void onNotificationResponse(const NotificationSyncResponse& response) = 0;

    /**
     * Updates the subscription state of voluntary topic
     *
     * @param commands the info about voluntary topic subscription.
     * @see SubscriptionCommand
     * @see SubscriptionCommands
     */
    virtual void onSubscriptionChanged(const SubscriptionCommands& commands) = 0;

    /**
     * Sets the given Notification processor.
     *
     * @param processor the Notification processor which to be set.
     * @see INotificationProcessor
     *
     */
    virtual void setNotificationProcessor(INotificationProcessor* processor) = 0;

    virtual ~INotificationTransport() {}
};

} /* namespace kaa */

#endif /* INOTIFICATIONTRANSPORT_HPP_ */
