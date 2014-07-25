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

package org.kaaproject.kaa.client.channel;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.junit.Assert;
import org.junit.Test;
import org.kaaproject.kaa.client.AbstractKaaClient;
import org.kaaproject.kaa.client.channel.impl.channels.DefaultOperationHttpChannel;
import org.kaaproject.kaa.client.persistence.KaaClientState;
import org.kaaproject.kaa.client.transport.AbstractHttpClient;
import org.kaaproject.kaa.common.TransportType;
import org.kaaproject.kaa.common.bootstrap.gen.ChannelType;
import org.kaaproject.kaa.common.endpoint.security.KeyUtil;
import org.kaaproject.kaa.common.endpoint.security.MessageEncoderDecoder;
import org.mockito.Mockito;

public class DefaultOperationHttpChannelTest {

    private static final Map<TransportType, ChannelDirection> SUPPORTED_TYPES = new HashMap<TransportType, ChannelDirection>();
    static {
        SUPPORTED_TYPES.put(TransportType.EVENT, ChannelDirection.UP);
        SUPPORTED_TYPES.put(TransportType.LOGGING, ChannelDirection.UP);
    }

    public ExecutorService fakeExecutor = new FakeExecutorService();

    class DefaultOperationHttpChannelFake extends DefaultOperationHttpChannel {

        private final int wantedNumberOfInvocations;

        public DefaultOperationHttpChannelFake(AbstractKaaClient client,
                KaaClientState state, int wantedNumberOfInvocations) {
            super(client, state);
            this.wantedNumberOfInvocations = wantedNumberOfInvocations;
        }

        @Override
        protected ExecutorService createExecutor() {
            super.createExecutor();
            return fakeExecutor;
        }

        public void verify() throws Exception {
            Mockito.verify(getMultiplexer(), Mockito.times(wantedNumberOfInvocations)).compileRequest(Mockito.anyMap());
            Mockito.verify(getDemultiplexer(), Mockito.times(wantedNumberOfInvocations)).processResponse(Mockito.any(byte [].class));
        }
    }

    @Test
    public void testChannelGetters() {
        AbstractKaaClient client = Mockito.mock(AbstractKaaClient.class);
        KaaClientState state = Mockito.mock(KaaClientState.class);
        KaaDataChannel channel = new DefaultOperationHttpChannel(client, state);

        Assert.assertEquals(SUPPORTED_TYPES, channel.getSupportedTransportTypes());
        Assert.assertEquals(ChannelType.HTTP, channel.getType());
        Assert.assertEquals("default_operations_http_channel", channel.getId());
    }

    @Test
    public void testChannelSync() throws Exception {
        KaaChannelManager manager = Mockito.mock(KaaChannelManager.class);
        AbstractHttpClient httpClient = Mockito.mock(AbstractHttpClient.class);
        Mockito.when(
                httpClient.executeHttpRequest(Mockito.anyString(),
                        Mockito.any(LinkedHashMap.class))).thenReturn(
                new byte[] { 5, 5, 5 });

        MessageEncoderDecoder encDec = Mockito.mock(MessageEncoderDecoder.class);
        Mockito.when(httpClient.getEncoderDecoder()).thenReturn(encDec);

        AbstractKaaClient client = Mockito.mock(AbstractKaaClient.class);
        Mockito.when(
                client.createHttpClient(Mockito.anyString(),
                        Mockito.any(PrivateKey.class),
                        Mockito.any(PublicKey.class),
                        Mockito.any(PublicKey.class))).thenReturn(httpClient);

        Mockito.when(client.getChannelMananager()).thenReturn(manager);

        KaaClientState state = Mockito.mock(KaaClientState.class);
        KaaDataMultiplexer multiplexer = Mockito.mock(KaaDataMultiplexer.class);
        KaaDataDemultiplexer demultiplexer = Mockito.mock(KaaDataDemultiplexer.class);
        DefaultOperationHttpChannelFake channel = new DefaultOperationHttpChannelFake(client, state, 2);

        HttpServerInfo server = new HttpServerInfo("localhost", 9889, KeyUtil.generateKeyPair().getPublic());

        channel.setServer(server);

        channel.sync(TransportType.EVENT);
        channel.setDemultiplexer(demultiplexer);
        channel.setDemultiplexer(null);
        channel.sync(TransportType.EVENT);
        channel.setMultiplexer(multiplexer);
        channel.setMultiplexer(null);
        channel.sync(TransportType.BOOTSTRAP);
        channel.sync(TransportType.EVENT);

        channel.syncAll();

        channel.verify();
    }

    @Test
    public void testServerFailed() throws Exception {
        KaaChannelManager manager = Mockito.mock(KaaChannelManager.class);
        MessageEncoderDecoder encDec = Mockito.mock(MessageEncoderDecoder.class);
        AbstractHttpClient httpClient = Mockito.mock(AbstractHttpClient.class);
        Mockito.when(
                httpClient.executeHttpRequest(Mockito.anyString(),
                        Mockito.any(LinkedHashMap.class))).thenThrow(new Exception());
        Mockito.when(httpClient.getEncoderDecoder()).thenReturn(encDec);

        AbstractKaaClient client = Mockito.mock(AbstractKaaClient.class);
        Mockito.when(
                client.createHttpClient(Mockito.anyString(),
                        Mockito.any(PrivateKey.class),
                        Mockito.any(PublicKey.class),
                        Mockito.any(PublicKey.class))).thenReturn(httpClient);

        Mockito.when(client.getChannelMananager()).thenReturn(manager);

        KaaClientState state = Mockito.mock(KaaClientState.class);
        KaaDataMultiplexer multiplexer = Mockito.mock(KaaDataMultiplexer.class);
        KaaDataDemultiplexer demultiplexer = Mockito.mock(KaaDataDemultiplexer.class);
        DefaultOperationHttpChannelFake channel = new DefaultOperationHttpChannelFake(client, state, 1);

        HttpServerInfo server = new HttpServerInfo("localhost", 9889, KeyUtil.generateKeyPair().getPublic());

        channel.sync(TransportType.EVENT);
        channel.setDemultiplexer(demultiplexer);
        channel.sync(TransportType.EVENT);
        channel.setMultiplexer(multiplexer);
        channel.sync(TransportType.BOOTSTRAP);
        channel.sync(TransportType.EVENT);
        channel.syncAll();

        channel.setServer(server);

        Mockito.verify(manager, Mockito.times(1)).onServerFailed(server);
    }

    @Test
    public void testShutdown() throws Exception {
        KaaChannelManager manager = Mockito.mock(KaaChannelManager.class);
        AbstractHttpClient httpClient = Mockito.mock(AbstractHttpClient.class);
        Mockito.when(
                httpClient.executeHttpRequest(Mockito.anyString(),
                        Mockito.any(LinkedHashMap.class))).thenThrow(new Exception());

        AbstractKaaClient client = Mockito.mock(AbstractKaaClient.class);
        Mockito.when(
                client.createHttpClient(Mockito.anyString(),
                        Mockito.any(PrivateKey.class),
                        Mockito.any(PublicKey.class),
                        Mockito.any(PublicKey.class))).thenReturn(httpClient);

        Mockito.when(client.getChannelMananager()).thenReturn(manager);

        KaaClientState state = Mockito.mock(KaaClientState.class);
        KaaDataMultiplexer multiplexer = Mockito.mock(KaaDataMultiplexer.class);
        KaaDataDemultiplexer demultiplexer = Mockito.mock(KaaDataDemultiplexer.class);
        DefaultOperationHttpChannelFake channel = new DefaultOperationHttpChannelFake(client, state, 0);
        channel.syncAll();
        channel.setDemultiplexer(demultiplexer);
        channel.setMultiplexer(multiplexer);
        channel.shutdown();

        HttpServerInfo server = new HttpServerInfo("localhost", 9889, KeyUtil.generateKeyPair().getPublic());
        channel.setServer(server);

        channel.sync(TransportType.EVENT);
        channel.syncAll();

        channel.verify();
    }

}
