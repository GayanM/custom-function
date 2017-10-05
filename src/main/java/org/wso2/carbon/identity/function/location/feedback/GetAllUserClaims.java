/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.function.location.feedback;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;

import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GetAllUserClaims implements GetAll {

    private static final Log log = LogFactory.getLog(GetAllUserClaims.class);
    private RealmService realmService;

    public GetAllUserClaims(RealmService realmService) {
        this.realmService = realmService;
    }

    public Claim[] getAll(AuthenticationContext context) {

        AuthenticatedUser authenticatedUser = context.getLastAuthenticatedUser();
        if (authenticatedUser == null) {
            return new Claim[0];
        }

        try {
            String tenantDomain = context.getTenantDomain();
            UserRealm userRealm = getUserRealm(tenantDomain);
            if (userRealm != null) {
                UserStoreManager userStore = getUserStoreManager(tenantDomain, userRealm,
                        authenticatedUser.getUserStoreDomain());
                Claim[] claims = userStore.getUserClaimValues(authenticatedUser.getUserName(),"default");
                if (!ArrayUtils.isEmpty(claims)) {
                    return claims;
                }
            }
        } catch (FrameworkException e) {
            //TODO: Handle exception, Needs support from our Javascript Engine
            log.error("Error in evaluating the function ", e);
        } catch (UserStoreException e) {
            //TODO: Handle exception. Needs support from our Javascript Engine
            log.error("Error in getting user from store at the function ", e);
        }
        return new Claim[0];
    }

    private UserRealm getUserRealm(String tenantDomain) throws FrameworkException, UserStoreException {

        int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
        return (UserRealm)realmService.getTenantUserRealm(tenantId);
    }

    private UserStoreManager getUserStoreManager(String tenantDomain, UserRealm realm, String userDomain)
            throws FrameworkException {
        UserStoreManager userStore = null;
        try {
            userStore = realm.getUserStoreManager();
            if (StringUtils.isNotBlank(userDomain)) {
                userStore = realm.getUserStoreManager().getSecondaryUserStoreManager(userDomain);
            }

            if (userStore == null) {
                // To avoid NPEs
                throw new FrameworkException(
                        "Invalid user store domain name : " + userDomain + " in tenant : " + tenantDomain);
            }
        } catch (UserStoreException e) {
            throw new FrameworkException(
                    "Error occurred while retrieving the UserStoreManager " + "from Realm for " + tenantDomain
                            + " to handle local claims", e);
        }
        return userStore;
    }
}
