package com.nahora.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompDecoder;
import org.springframework.util.MultiValueMap;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * StompDecoder that tolerates missing null byte terminators in STOMP frames.
 * React Native's OkHttp WebSocket strips trailing null bytes from text messages.
 */
public class LenientStompDecoder extends StompDecoder {

    @Override
    public List<Message<byte[]>> decode(ByteBuffer byteBuffer) {
        return decode(byteBuffer, null);
    }

    @Override
    public List<Message<byte[]>> decode(ByteBuffer byteBuffer, MultiValueMap<String, String> headers) {
        if (containsNullByte(byteBuffer)) {
            return super.decode(byteBuffer, headers);
        }

        // Append missing null byte terminator for React Native / OkHttp compatibility
        ByteBuffer patched = ByteBuffer.allocate(byteBuffer.remaining() + 1);
        patched.put(byteBuffer);
        patched.put((byte) 0);
        patched.flip();
        return super.decode(patched, headers);
    }

    private boolean containsNullByte(ByteBuffer buffer) {
        for (int i = buffer.position(); i < buffer.limit(); i++) {
            if (buffer.get(i) == 0) {
                return true;
            }
        }
        return false;
    }
}
