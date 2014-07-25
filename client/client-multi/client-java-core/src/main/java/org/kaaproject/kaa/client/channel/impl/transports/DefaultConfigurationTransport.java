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

import java.io.IOException;
import java.nio.ByteBuffer;

import org.kaaproject.kaa.client.channel.ConfigurationTransport;
import org.kaaproject.kaa.client.configuration.ConfigurationHashContainer;
import org.kaaproject.kaa.client.configuration.ConfigurationProcessor;
import org.kaaproject.kaa.client.schema.SchemaProcessor;
import org.kaaproject.kaa.common.TransportType;
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
import org.kaaproject.kaa.common.endpoint.gen.ConfigurationSyncRequest;
import org.kaaproject.kaa.common.endpoint.gen.ConfigurationSyncResponse;
import org.kaaproject.kaa.common.endpoint.gen.SyncResponseStatus;
import org.kaaproject.kaa.common.hash.EndpointObjectHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of the {@link ConfigurationTransport}.
 *
 * @author Yaroslav Zeygerman
 *
 */
public class DefaultConfigurationTransport extends AbstractKaaTransport implements ConfigurationTransport {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultConfigurationTransport.class);

    private ConfigurationHashContainer hashContainer;
    private ConfigurationProcessor configProcessor;
    private SchemaProcessor schemaProcessor;

    @Override
    public void sync() {
        syncByType(TransportType.CONFIGURATION);
    }

    @Override
    public void setConfigurationHashContainer(ConfigurationHashContainer container) {
        this.hashContainer = container;
    }

    @Override
    public void setConfigurationProcessor(ConfigurationProcessor processor) {
        this.configProcessor = processor;
    }

    @Override
    public void setSchemaProcessor(SchemaProcessor processor) {
        this.schemaProcessor = processor;
    }

    @Override
    public ConfigurationSyncRequest createConfigurationRequest() {
        if (clientState != null && hashContainer != null) {
            EndpointObjectHash hash = hashContainer.getConfigurationHash();
            clientState.setConfigurationHash(hash);
            ConfigurationSyncRequest request = new ConfigurationSyncRequest();
            if (hash != null) {
                request.setConfigurationHash(ByteBuffer.wrap(hash.getData()));
            }
            request.setAppStateSeqNumber(clientState.getConfigSeqNumber());
            return request;
        }
        return null;
    }

    @Override
    public void onConfigurationResponse(ConfigurationSyncResponse response) throws IOException {
        if (clientState != null && configProcessor != null && schemaProcessor != null) {
            clientState.setConfigSeqNumber(response.getAppStateSeqNumber());
            ByteBuffer schemaBody = response.getConfSchemaBody();
            if (schemaBody != null) {
                schemaProcessor.loadSchema(schemaBody);
            }
            ByteBuffer confBody = response.getConfDeltaBody();
            if (confBody != null) {
                configProcessor.processConfigurationData(confBody, response.getResponseStatus().equals(SyncResponseStatus.RESYNC));
            }
            LOG.info("Processed configuration response");
        }
    }

}
