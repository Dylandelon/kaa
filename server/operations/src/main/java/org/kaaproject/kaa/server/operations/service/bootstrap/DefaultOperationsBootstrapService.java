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

package org.kaaproject.kaa.server.operations.service.bootstrap;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.curator.retry.RetryUntilElapsed;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.server.TThreadPoolServer.Args;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.kaaproject.kaa.server.common.thrift.gen.operations.OperationsThriftService;
import org.kaaproject.kaa.server.common.zk.ZkChannelException;
import org.kaaproject.kaa.server.common.zk.gen.ConnectionInfo;
import org.kaaproject.kaa.server.common.zk.gen.OperationsNodeInfo;
import org.kaaproject.kaa.server.common.zk.gen.SupportedChannel;
import org.kaaproject.kaa.server.common.zk.operations.OperationsNode;
import org.kaaproject.kaa.server.operations.service.OperationsService;
import org.kaaproject.kaa.server.operations.service.akka.AkkaService;
import org.kaaproject.kaa.server.operations.service.cache.CacheService;
import org.kaaproject.kaa.server.operations.service.config.NettyHttpServiceChannelConfig;
import org.kaaproject.kaa.server.operations.service.config.OperationsServerConfig;
import org.kaaproject.kaa.server.operations.service.config.ServiceChannelConfig;
import org.kaaproject.kaa.server.operations.service.event.EventService;
import org.kaaproject.kaa.server.operations.service.http.HttpService;
import org.kaaproject.kaa.server.operations.service.security.KeyStoreService;
import org.kaaproject.kaa.server.operations.service.statistics.StatisticsFactory;
import org.kaaproject.kaa.server.operations.service.statistics.StatisticsService;
import org.kaaproject.kaa.server.operations.service.thrift.OperationsThriftServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The Class for Operations bootstrap process.
 * Main methods are {@link #start() start} and {@link #stop() stop}
 * Launches Transport and ZK services
 *
 * @author ashvayka
 */
@Service
public class DefaultOperationsBootstrapService implements OperationsBootstrapService {

    /** The Constant logger. */
    private static final Logger LOG = LoggerFactory
            .getLogger(DefaultOperationsBootstrapService.class);

    /** The server. */
    private TServer server;

    /** The operations node. */
    private OperationsNode operationsNode;

    /** The Service Channels List */
    private List<ServiceChannel> serviceChannels;

    /** The operations thrift service. */
    @Autowired
    private OperationsThriftServiceImpl operationsThriftService;

    /** The key store service. */
    @Autowired
    private KeyStoreService keyStoreService;

    /** The operations service. */
    @Autowired
    private OperationsService operationsService;

    /** The Akka service. */
    @Autowired
    private AkkaService akkaService;

    /** The cache service. */
    @Autowired
    private CacheService cacheService;

    /** The operations server config. */
    @Autowired
    private OperationsServerConfig operationsServerConfig;

    /** The event service */
    @Autowired
    private EventService eventService;

    /* (non-Javadoc)
     * @see org.kaaproject.kaa.server.operations.service.bootstrap.OperationsBootstrapService#getEndpointService()
     */
    @Override
    public OperationsService getOperationsService() {
        return operationsService;
    }

    /* (non-Javadoc)
     * @see org.kaaproject.kaa.server.operations.service.bootstrap.OperationsBootstrapService#getKeyStoreService()
     */
    @Override
    public KeyStoreService getKeyStoreService() {
        return keyStoreService;
    }

    /* (non-Javadoc)
     * @see org.kaaproject.kaa.server.operations.service.bootstrap.OperationsBootstrapService#getCacheService()
     */
    @Override
    public CacheService getCacheService() {
        return cacheService;
    }


    /**
     * OperationsServerConfig getter
     * @return OperationsServerConfig
     */
    private OperationsServerConfig getConfig() {
        return operationsServerConfig;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.kaaproject.kaa.server.operations.service.bootstrap.
     * OperationsBootstrapService#start()
     */
    @Override
    public void start() {

        operationsService.setPublicKey(keyStoreService.getPublicKey());
        eventService.setConfig(getConfig());
        operationsThriftService.setEventService(eventService);
        
        for(ServiceChannelConfig channelConf : getConfig().getChannelList()) {
            LOG.info("Channel {} initializing....", channelConf.getChannelType().toString());
            switch (channelConf.getChannelType()) {
            case HTTP:
            case HTTP_LP:
                StatisticsService statistics = StatisticsFactory.getService(channelConf.getChannelType());
                ((NettyHttpServiceChannelConfig)channelConf).setSessionTrack(statistics);
                ((NettyHttpServiceChannelConfig)channelConf).setOperationServerConfig(getConfig());
                ServiceChannel channel = new HttpService((NettyHttpServiceChannelConfig)channelConf);
                channel.start();
                //TODO need to added blocking while channels starts to check if all is OK
                serviceChannels.add(channel);
                break;
            default:
                LOG.error("Channel {} unrecognized....",channelConf.getChannelType().toString());
                break;
            }
        }
        if (!serviceChannels.isEmpty()) {

            if (getConfig().isZkEnabled()) {
                startZK();
            }

            // Blocking method call
            startThrift();
        } else {
            LOG.error("Operations start failed, No one Service Channels started...");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.kaaproject.kaa.server.operations.service.bootstrap.
     * OperationsBootstrapService#stop()
     */
    @Override
    public void stop() {
        for(ServiceChannel channel : serviceChannels) {
            channel.stop();
        }
        serviceChannels.clear();
        StatisticsFactory.shutdown();
        if( akkaService != null){
            akkaService.getActorSystem().shutdown();
        }
        if (getConfig().isZkEnabled()) {
            stopZK();
        }
        // Thrift stop
        server.stop();
    }

    /**
     * Stop zk node.
     */
    private void stopZK() {
        try {
            operationsNode.close();
        } catch (IOException e) {
            LOG.warn("Error closing ZK node", e);
        }
    }

    /**
     * Start thrift service.
     */
    private void startThrift() {

        LOG.info("Initializing Thrift Service for Operations Server....");
        LOG.info("thrift host: {}", getConfig().getThriftHost());
        LOG.info("thrift port: {}", getConfig().getThriftPort());

        try {
            OperationsThriftService.Processor<OperationsThriftService.Iface> processor = new OperationsThriftService.Processor<OperationsThriftService.Iface>(
                    operationsThriftService);
            TServerTransport serverTransport = new TServerSocket(new InetSocketAddress(getConfig().getThriftHost(), getConfig().getThriftPort()));
            
            TThreadPoolServer.Args args = new Args(serverTransport).processor(processor);
            args.stopTimeoutVal = 3;
            args.stopTimeoutUnit = TimeUnit.SECONDS;
            server = new TThreadPoolServer(args);

            LOG.info("Operations Server Started.");

            server.serve();

            LOG.info("Operations Server Stopped.");

        } catch (TTransportException e) {
            LOG.error("TTransportException", e);
        }
    }

    /**
     * Start zk node.
     */
    private void startZK() {
        OperationsNodeInfo nodeInfo = new OperationsNodeInfo();
        ByteBuffer keyData = ByteBuffer.wrap(keyStoreService.getPublicKey().getEncoded());
        ConnectionInfo connectionInfo = new ConnectionInfo(getConfig().getThriftHost(), getConfig().getThriftPort(), keyData);
        nodeInfo.setConnectionInfo(connectionInfo);
        List<SupportedChannel> suppChannels = new ArrayList<>();
        for(ServiceChannel sc : serviceChannels) {
            try {
                suppChannels.add(sc.getZkSupportedChannel());
            } catch (ZkChannelException e) {
                LOG.error("Error advertize Channel ",e);
            }
        }
        nodeInfo.setSupportedChannelsArray(suppChannels );
        operationsNode = new OperationsNode(nodeInfo, getConfig().getZkHostPortList(), new RetryUntilElapsed(getConfig().getZkMaxRetryTime(), getConfig().getZkSleepTime()));
        try {
            operationsNode.start();
            getConfig().setZkNode(operationsNode);
            eventService.setZkNode(operationsNode);
        } catch (Exception e) {
            if (getConfig().isZkIgnoreErrors()) {
                LOG.info("Failed to register operations in ZooKeeper", e);
            } else {
                LOG.error("Failed to register operations in ZooKeeper", e);
                throw new RuntimeException(e); //NOSONAR
            }
        }
    }

    /**
     * 
     */
    public DefaultOperationsBootstrapService() {
        serviceChannels = new ArrayList<>();
    }

}
