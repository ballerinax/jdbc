/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 *
 */

package org.ballerinalang.nativeimpl.io.channels.base;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Represents a channel which will allow to perform data i/o
 */
public class DataChannel {
    /**
     * Source for reading bytes
     */
    private Channel channel;
    /**
     * Represents the byte order
     */
    private ByteOrder order;

    public DataChannel(Channel channel, ByteOrder order) {
        this.channel = channel;
        this.order = order;
    }

    private void readFull(ByteBuffer buffer) throws IOException {
        do {
            channel.read(buffer);
        } while (buffer.hasRemaining() && !channel.hasReachedEnd());
    }

    private long decodeLong(Representation representation) throws IOException {
        ByteBuffer buffer;
        long value = 0;
        int totalNumberOfBits;
        int requiredNumberOfBytes;
        int maxNumberOfBits = 0xFFFF;
        if (Representation.VARIABLE.equals(representation)) {
            throw new UnsupportedOperationException();
        } else {
            requiredNumberOfBytes = representation.getNumberOfBytes();
            buffer = ByteBuffer.allocate(requiredNumberOfBytes);
            buffer.order(order);
        }
        readFull(buffer);
        buffer.flip();
        totalNumberOfBits = (buffer.limit() - 1) * Byte.SIZE;
        do {
            long shiftedValue = 0L;
            if (Representation.BIT_64.equals(representation)) {
                long flippedValue = (buffer.get() & maxNumberOfBits);
                shiftedValue = flippedValue << totalNumberOfBits;
            } else if (Representation.BIT_32.equals(representation)) {
                int flippedValue = (buffer.get() & maxNumberOfBits);
                shiftedValue = flippedValue << totalNumberOfBits;
            } else if (Representation.BIT_16.equals(representation)) {
                short flippedValue = (short) (buffer.get() & maxNumberOfBits);
                shiftedValue = flippedValue << totalNumberOfBits;
            }
            maxNumberOfBits = 0xFF;
            value = value + shiftedValue;
            totalNumberOfBits = totalNumberOfBits - Byte.SIZE;
        } while (buffer.hasRemaining());
        return value;
    }

    private byte[] encodeLong(long value, Representation representation) {
        byte[] content;
        int nBytes;
        int totalNumberOfBits;
        if (Representation.VARIABLE.equals(representation)) {
            nBytes = (int) Math.abs(Math.round((Math.log(Math.abs(value)) / Math.log(2)) / Byte.SIZE));
            content = new byte[nBytes];
        } else {
            nBytes = representation.getNumberOfBytes();
            content = new byte[representation.getNumberOfBytes()];
        }
        totalNumberOfBits = (nBytes * Byte.SIZE) - Byte.SIZE;
        for (int count = 0; count < nBytes; count++) {
            content[count] = (byte) (value >> totalNumberOfBits);
            totalNumberOfBits = totalNumberOfBits - Byte.SIZE;
        }
        return content;
    }

    public void writeFixedLong(long value, Representation representation) throws IOException {
        byte[] bytes = encodeLong(value, representation);
        channel.write(ByteBuffer.wrap(bytes));
    }

    public long readFixedLong(Representation representation) throws IOException {
        return decodeLong(representation);
    }

    public void writeDouble(double value, Representation representation) throws IOException {
        long lValue;
        if (Representation.BIT_32.equals(representation)) {
            lValue = Float.floatToIntBits((float) value);
        } else {
            lValue = Double.doubleToRawLongBits(value);
        }
        writeFixedLong(lValue, representation);
    }

    public double readDouble(Representation representation) throws IOException {
        if (Representation.BIT_32.equals(representation)) {
            int fValue = (int) readFixedLong(Representation.BIT_32);
            return Float.intBitsToFloat(fValue);
        } else {
            long lValue = readFixedLong(representation);
            return Double.longBitsToDouble(lValue);
        }
    }

    public void writeBoolean(boolean value) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        byte booleanValue = (byte) (value ? 1 : 0);
        buffer.put(booleanValue);
        channel.write(buffer);
    }

    public boolean readBoolean() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        readFull(buffer);
        buffer.flip();
        return buffer.get() == 1;
    }
}
