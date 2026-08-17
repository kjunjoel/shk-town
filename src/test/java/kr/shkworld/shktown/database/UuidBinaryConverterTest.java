package kr.shkworld.shktown.database;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UuidBinaryConverterTest {
    @Test
    void convertsUuidBothWays() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, UuidBinaryConverter.fromBytes(UuidBinaryConverter.toBytes(uuid)));
    }

    @Test
    void rejectsInvalidByteLength() {
        assertThrows(IllegalArgumentException.class, () ->
                UuidBinaryConverter.fromBytes(new byte[15]));
    }
}
