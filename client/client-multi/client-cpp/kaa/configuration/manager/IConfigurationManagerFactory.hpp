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

#ifndef I_CONFIGURATION_MANAGER_FACTORY_HPP_
#define I_CONFIGURATION_MANAGER_FACTORY_HPP_

#include "kaa/configuration/manager/IConfigurationManager.hpp"

namespace kaa {

class IConfigurationManagerFactory {
public:
    virtual IConfigurationManager & getConfigurationManager() = 0;

    virtual ~IConfigurationManagerFactory() {}
};

}  // namespace kaa


#endif /* I_CONFIGURATION_MANAGER_FACTORY_HPP_ */
