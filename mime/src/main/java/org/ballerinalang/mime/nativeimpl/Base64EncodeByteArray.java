/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ballerinalang.mime.nativeimpl;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BByteArray;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.ReturnType;

import static org.ballerinalang.stdlib.io.utils.Utils.encodeBlob;

/**
 * Extern function for Base64Encode byte array.
 *
 * @since 0.980.0
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "mime",
        functionName = "base64EncodeByteArray",
        args = {@Argument(name = "b", type = TypeKind.ARRAY, elementType = TypeKind.BYTE)},
        returnType = {@ReturnType(type = TypeKind.ARRAY, elementType = TypeKind.BYTE)},
        isPublic = true
)
public class Base64EncodeByteArray extends BlockingNativeCallableUnit {

    @Override
    public void execute(Context context) {
        byte[] byteArray = ((BByteArray) context.getRefArgument(0)).getBytes();
        encodeBlob(context, byteArray, false);
    }
}
