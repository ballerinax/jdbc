/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.test.service.grpc;

import org.ballerinalang.test.IntegrationTestCase;
import org.ballerinalang.test.context.BallerinaTestException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;

import java.io.File;

public class GrpcBaseTest extends IntegrationTestCase {
    @BeforeGroups("grpc-test")
    public void start() throws BallerinaTestException {
        String balFile = new File("src" + File.separator + "test" + File.separator + "resources" + File.separator +
                "grpc").getAbsolutePath();
        String[] args = new String[] {"--sourceroot", balFile};
        serverInstance.startBallerinaServer("grpcServices", args);
    }

    @AfterGroups("grpc-test")
    public void cleanup() throws Exception {
        serverInstance.removeAllLeechers();
        serverInstance.stopServer();
    }
}