
package com.poorvi.url_shortener.url.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    @Test
    void shouldEncodeZero() {
        assertEquals("0", Base62Encoder.encode(0));
    }

    @Test
    void shouldEncodeOne() {
        assertEquals("1", Base62Encoder.encode(1));
    }

    @Test
    void shouldEncodeSixtyTwo() {
        assertEquals("10", Base62Encoder.encode(62));
    }

    @Test
    void shouldEncodeNegativeNumber() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Base62Encoder.encode(-1)
        );
    }
}
