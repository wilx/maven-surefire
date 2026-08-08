/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.surefire.booter.stream;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.Test;

import static java.nio.CharBuffer.wrap;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.surefire.api.util.internal.Channels.newBufferedChannel;
import static org.assertj.core.api.Assertions.assertThat;

class EventEncoderTest {
    @Test
    void shouldReuseResetEncoder() {
        EventEncoder eventEncoder = newEventEncoder();
        CharsetEncoder encoder = eventEncoder.newCharsetEncoder();
        assertThat(encoder.encode(wrap("first"), ByteBuffer.allocate(32), true).isUnderflow())
                .isTrue();

        eventEncoder.returnCharsetEncoder(encoder);
        CharsetEncoder reusedEncoder = eventEncoder.newCharsetEncoder();
        try {
            assertThat(reusedEncoder).isSameAs(encoder);
            assertThat(reusedEncoder
                            .encode(wrap("second"), ByteBuffer.allocate(32), false)
                            .isUnderflow())
                    .isTrue();
        } finally {
            eventEncoder.returnCharsetEncoder(reusedEncoder);
        }
    }

    @Test
    void shouldDiscardEncoderWithDifferentCharset() {
        EventEncoder eventEncoder = newEventEncoder();
        CharsetEncoder differentEncoder = UTF_16.newEncoder();
        eventEncoder.returnCharsetEncoder(differentEncoder);

        CharsetEncoder encoder = eventEncoder.newCharsetEncoder();
        try {
            assertThat(encoder).isNotSameAs(differentEncoder);
            assertThat(encoder.charset()).isEqualTo(UTF_8);
        } finally {
            eventEncoder.returnCharsetEncoder(encoder);
        }
    }

    @Test
    void shouldKeepEncodersIsolatedByThread() throws Exception {
        EventEncoder eventEncoder = newEventEncoder();
        CharsetEncoder currentThreadEncoder = eventEncoder.newCharsetEncoder();
        eventEncoder.returnCharsetEncoder(currentThreadEncoder);

        FutureTask<CharsetEncoder> otherThreadEncoder = new FutureTask<>(eventEncoder::newCharsetEncoder);
        Thread thread = new Thread(otherThreadEncoder);
        thread.start();
        thread.join();

        assertThat(otherThreadEncoder.get()).isNotSameAs(currentThreadEncoder);

        CharsetEncoder reusedEncoder = eventEncoder.newCharsetEncoder();
        try {
            assertThat(reusedEncoder).isSameAs(currentThreadEncoder);
        } finally {
            eventEncoder.returnCharsetEncoder(reusedEncoder);
        }
    }

    private static EventEncoder newEventEncoder() {
        return new EventEncoder(newBufferedChannel(new ByteArrayOutputStream()));
    }
}
