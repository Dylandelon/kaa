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

#ifndef SETVALUEHELPER_HPP_
#define SETVALUEHELPER_HPP_

#include <avro/Generic.hh>

namespace kaa {

struct SetAvroValueHelper {
    static bool setValue(avro::Type t, avro::GenericDatum &indatum, const avro::GenericDatum &field);
};

}  // namespace kaa


#endif /* SETVALUEHELPER_HPP_ */
