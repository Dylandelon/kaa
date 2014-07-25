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
package org.kaaproject.kaa.sandbox.web.services;

import org.kaaproject.kaa.common.dto.admin.SdkKey;
import org.kaaproject.kaa.sandbox.admin.AdminClient;
import org.kaaproject.kaa.sandbox.admin.FileData;
import org.kaaproject.kaa.sandbox.web.services.cache.CacheService;
import org.kaaproject.kaa.sandbox.web.services.rest.AdminClientProvider;
import org.kaaproject.kaa.sandbox.web.services.util.Utils;
import org.kaaproject.kaa.sandbox.web.shared.dto.ProjectDataKey;
import org.kaaproject.kaa.sandbox.web.shared.services.SandboxServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CacheServiceImpl implements CacheService {

    private static final String TENANT_DEVELOPER_USER = "devuser";
    private static final String TENANT_DEVELOPER_PASSWORD = "devuser123";
    
    @Autowired
    private AdminClientProvider clientProvider;
    
    @Override
    @Cacheable("sdkCache")
    public FileData getSdk(SdkKey key) throws SandboxServiceException {
        AdminClient client = clientProvider.getClient();
        client.login(TENANT_DEVELOPER_USER, TENANT_DEVELOPER_PASSWORD);
        FileData fileData;
        try {
            fileData = client.downloadSdk(key);
        } catch (Exception e) {
            throw Utils.handleException(e);
        }
        return fileData;
    }
    
    @Override
    @Cacheable(value = "fileCache", key = "#key", unless="#result == null")
    public FileData getProjectFile(ProjectDataKey key) {
        return null;
    }
    
    @Override
    @CachePut(value = "fileCache", key = "#key")
    public FileData putProjectFile(ProjectDataKey key, FileData data) {
        return data;
    }

}
