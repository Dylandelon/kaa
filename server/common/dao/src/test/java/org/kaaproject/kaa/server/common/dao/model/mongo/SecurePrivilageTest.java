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
package org.kaaproject.kaa.server.common.dao.model.mongo;

import java.util.ArrayList;
import java.util.List;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import org.junit.Assert;
import org.junit.Test;
import org.kaaproject.kaa.common.dto.logs.security.MongoPrivilegeDto;
import org.kaaproject.kaa.server.common.dao.model.mongo.SecurePrivilege;

public class SecurePrivilageTest {
    
    private static final SecureResource TEST_RESOURCE = new SecureResource();
    private static final List<String> TEST_ACTIONS = new ArrayList<>();

    @Test
    public void basicLogEventTest() {
        SecurePrivilege privilege = new SecurePrivilege();
        
        Assert.assertNull(privilege.getResource());
        Assert.assertNull(privilege.getActions());
        
        privilege.setResource(TEST_RESOURCE);
        privilege.setActions(TEST_ACTIONS);
        
        Assert.assertEquals(TEST_RESOURCE, privilege.getResource());
        Assert.assertEquals(TEST_ACTIONS, privilege.getActions());
        
        MongoPrivilegeDto dto = privilege.toDto();
        
        Assert.assertEquals(TEST_ACTIONS, dto.getActions());
    }
    
    @Test
    public void hashCodeEqualsTest(){
        EqualsVerifier.forClass(SecurePrivilege.class).suppress(Warning.NONFINAL_FIELDS).verify();
    }

}
