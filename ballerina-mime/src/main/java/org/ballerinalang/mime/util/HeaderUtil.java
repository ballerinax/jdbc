/*
*  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/

package org.ballerinalang.mime.util;

import org.ballerinalang.bre.Context;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BStringArray;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.StructInfo;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.jvnet.mimepull.Header;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.ballerinalang.mime.util.Constants.BUILTIN_PACKAGE;
import static org.ballerinalang.mime.util.Constants.ENTITY_HEADERS_INDEX;
import static org.ballerinalang.mime.util.Constants.FIRST_ELEMENT;
import static org.ballerinalang.mime.util.Constants.SEMICOLON;
import static org.ballerinalang.mime.util.Constants.STRUCT_GENERIC_ERROR;

/**
 * Utility methods for parsing headers.
 *
 * @since 0.967
 */
public class HeaderUtil {

    /**
     * Given a header value, get it's parameters.
     *
     * @param headerValue Header value as a string
     * @return Parameter map
     */
    public static BMap<String, BValue> getParamMap(String headerValue) {
        BMap<String, BValue> paramMap = null;
        if (headerValue.contains(SEMICOLON)) {
            extractValue(headerValue);
            List<String> paramList = Arrays.stream(headerValue.substring(headerValue.indexOf(SEMICOLON) + 1)
                    .split(SEMICOLON)).map(String::trim).collect(Collectors.toList());
            paramMap = validateParams(paramList) ? createParamBMap(paramList) : null;
        }
        return paramMap;
    }

    /**
     * Get header value without parameters.
     *
     * @param headerValue Header value with parameters as a string
     * @return Header value without parameters
     */
    public static String getHeaderValue(String headerValue) {
        return extractValue(headerValue.trim());
    }

    /**
     * Extract header value.
     *
     * @param headerValue Header value with parameters as a string
     * @return Header value without parameters
     */
    private static String extractValue(String headerValue) {
        String value = headerValue.substring(0, headerValue.indexOf(SEMICOLON)).trim();
        if (value.isEmpty()) {
            throw new BallerinaException("invalid header value: " + headerValue);
        }
        return value;
    }

    private static boolean validateParams(List<String> paramList) {
        //validate header values which ends with semicolon without params
        return !(paramList.size() == 1 && paramList.get(0).isEmpty());
    }

    /**
     * Get parser error as a ballerina struct.
     *
     * @param context Represent ballerina context
     * @param errMsg  Error message in string form
     * @return Ballerina struct with parse error
     */
    public static BStruct getParserError(Context context, String errMsg) {
        PackageInfo errorPackageInfo = context.getProgramFile().getPackageInfo(BUILTIN_PACKAGE);
        StructInfo errorStructInfo = errorPackageInfo.getStructInfo(STRUCT_GENERIC_ERROR);

        BStruct parserError = new BStruct(errorStructInfo.getType());
        parserError.setStringField(0, errMsg);
        return parserError;
    }

    /**
     * Create a parameter map.
     *
     * @param paramList List of parameters
     * @return Ballerina map
     */
    private static BMap<String, BValue> createParamBMap(List<String> paramList) {
        BMap<String, BValue> paramMap = new BMap<>();
        for (String param : paramList) {
            if (param.contains("=")) {
                String[] keyValuePair = param.split("=");
                if (keyValuePair.length != 2 || keyValuePair[0].isEmpty()) {
                    throw new BallerinaException("invalid header parameter: " + param);
                }
                paramMap.put(keyValuePair[0].trim(), new BString(keyValuePair[1].trim()));
            } else {
                //handle when parameter value is optional
                paramMap.put(param.trim(), null);
            }
        }
        return paramMap;
    }

    static boolean isHeaderExist(List<String> headers) {
        return headers != null && headers.get(FIRST_ELEMENT) != null && !headers.get(FIRST_ELEMENT).isEmpty();
    }

    /**
     * Set body part headers.
     *
     * @param bodyPartHeaders Represent decoded mime part headers
     * @param headerMap       Represent ballerina header map
     * @return a populated ballerina map with body part headers
     */
    static BMap<String, BValue> setBodyPartHeaders(List<? extends Header> bodyPartHeaders,
                                                   BMap<String, BValue> headerMap) {
        for (final Header header : bodyPartHeaders) {
            if (headerMap.keySet().contains(header.getName())) {
                BStringArray valueArray = (BStringArray) headerMap.get(header.getName());
                valueArray.add(valueArray.size(), header.getValue());
            } else {
                BStringArray valueArray = new BStringArray(new String[]{header.getValue()});
                headerMap.put(header.getName(), valueArray);
            }
        }
        return headerMap;
    }

    /**
     * Extract the header value from a body part for a given header name.
     *
     * @param bodyPart   Represent a ballerina body part.
     * @param headerName Represent an http header name
     * @return a header value for the given header name
     */
    public static String getHeaderValue(BStruct bodyPart, String headerName) {
        BMap<String, BValue> headerMap = (bodyPart.getRefField(ENTITY_HEADERS_INDEX) != null) ?
                (BMap<String, BValue>) bodyPart.getRefField(ENTITY_HEADERS_INDEX) : null;
        if (headerMap != null) {
            BStringArray headerValue = (BStringArray) headerMap.get(headerName);
            return headerValue.get(0);
        }
        return null;
    }

    /**
     * Get the header value intact with parameters.
     *
     * @param headerValue Header value as a string
     * @param map         Represent a parameter map
     * @return Header value along with it's parameters as a string
     */
    static String appendHeaderParams(String headerValue, BMap map) {
        StringBuilder builder = new StringBuilder(headerValue);
        int index = 0;
        Set<String> keys = map.keySet();
        if (!keys.isEmpty()) {
            for (String key : keys) {
                BString paramValue = (BString) map.get(key);
                if (index == keys.size() - 1) {
                    builder.append(key).append("=").append(paramValue.toString());
                } else {
                    builder.append(key).append("=").append(paramValue.toString()).append(";");
                    index = index + 1;
                }
            }
        }
        return builder.toString();
    }

    /**
     * Add a given header name and a value to entity headers.
     *
     * @param entityHeaders A map of entity headers
     * @param headerName    Header name as a string
     * @param headerValue   Header value as a string
     */
    static void addToEntityHeaders(BMap<String, BValue> entityHeaders, String headerName, String headerValue) {
        if (entityHeaders.keySet().contains(headerName)) {
            BStringArray valueArray = (BStringArray) entityHeaders.get(headerName);
            valueArray.add(valueArray.size(), headerValue);
        } else {
            BStringArray valueArray = new BStringArray(new String[]{headerValue});
            entityHeaders.put(headerName, valueArray);
        }
    }
}
