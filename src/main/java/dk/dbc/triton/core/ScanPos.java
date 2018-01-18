/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.core;

public enum ScanPos {
    FIRST,
    LAST;

    public static ScanPos fromString(String s) {
        if (s == null) {
            throw new IllegalArgumentException("String cannot be null");
        }
        return ScanPos.valueOf(s.toUpperCase());
    }
}
