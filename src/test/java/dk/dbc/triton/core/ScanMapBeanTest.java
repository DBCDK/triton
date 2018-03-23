/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.triton.core;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ScanMapBeanTest {
    private final ScanMapBean scanMapBean = newScanMapBean();

    @Test
    void unknownCollection() {
        assertThat(scanMapBean.resolve("unknown", "mti"),
                is("mti"));
    }

    @Test
    void unknownIndex() {
        assertThat(scanMapBean.resolve("collection", "unknown"),
                is("unknown"));
    }

    @Test
    void knownIndex() {
        assertThat(scanMapBean.resolve("collection", "mti"),
                is("scan.mti"));
    }

    private static ScanMapBean newScanMapBean() {
        final ScanMapBean scanMapBean = new ScanMapBean();
        scanMapBean.collectionProperties.put("collection", new Properties());
        scanMapBean.collectionProperties.get("collection").put("mti", "scan.mti");
        return scanMapBean;
    }
}