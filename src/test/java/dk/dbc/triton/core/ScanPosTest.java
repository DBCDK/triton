/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.core;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


class ScanPosTest {
    @Test
    void fromStringThrowsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> ScanPos.fromString(null));
    }

    @Test
    void fromStringThrowsOnEmpty() {
        assertThrows(IllegalArgumentException.class, () -> ScanPos.fromString(""));
    }

    @Test
    void fromStringThrowsOnUnknown() {
        assertThrows(IllegalArgumentException.class, () -> ScanPos.fromString("middle"));
    }

    @Test
    void fromString() {
        assertThat("first", ScanPos.fromString("first"), is(ScanPos.FIRST));
        assertThat("last", ScanPos.fromString("last"), is(ScanPos.LAST));
    }
}