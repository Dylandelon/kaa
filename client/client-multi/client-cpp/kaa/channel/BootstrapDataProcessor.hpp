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

#ifndef BOOTSTRAPDATAPROCESSOR_HPP_
#define BOOTSTRAPDATAPROCESSOR_HPP_

#include "kaa/channel/IKaaDataMultiplexer.hpp"
#include "kaa/channel/IKaaDataDemultiplexer.hpp"
#include "kaa/common/AvroByteArrayConverter.hpp"
#include "kaa/gen/BootstrapGen.hpp"

#include <boost/shared_ptr.hpp>
#include "kaa/channel/transport/IBootstrapTransport.hpp"

namespace kaa {

typedef boost::shared_ptr<IBootstrapTransport>      IBootstrapTransportPtr;

class BootstrapDataProcessor : public IKaaDataMultiplexer, public IKaaDataDemultiplexer
{
public:
    BootstrapDataProcessor(IBootstrapTransportPtr);

    virtual std::vector<boost::uint8_t> compileRequest(const std::map<TransportType, ChannelDirection>& transportTypes);
    virtual void processResponse(const std::vector<boost::uint8_t> &response);
private:
    AvroByteArrayConverter<Resolve>                 requestConverter_;
    AvroByteArrayConverter<OperationsServerList>    responseConverter_;

    IBootstrapTransportPtr                          bootstrapTransport_;
};

}  // namespace kaa


#endif /* BOOTSTRAPDATAPROCESSOR_HPP_ */
