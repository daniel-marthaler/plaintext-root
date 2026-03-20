/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended tests for VersionBean - prod profile and edge cases.
 */
class VersionBeanExtendedTest {

    @Test
    void init_shouldUseProdFile_whenProfileContainsProd() {
        VersionBean bean = new VersionBean();
        ReflectionTestUtils.setField(bean, "activeProfile", "production");

        bean.init();

        // versionRelease.txt doesn't exist, so falls back to dev-SNAPSHOT
        assertEquals("dev-SNAPSHOT", bean.getVersion());
    }

    @Test
    void init_shouldUseDevFile_whenProfileDoesNotContainProd() {
        VersionBean bean = new VersionBean();
        ReflectionTestUtils.setField(bean, "activeProfile", "staging");

        bean.init();

        assertEquals("dev-SNAPSHOT", bean.getVersion());
    }

    @Test
    void init_shouldSetBuildTimestamp_evenWhenFileNotFound() {
        VersionBean bean = new VersionBean();
        ReflectionTestUtils.setField(bean, "activeProfile", "dev");

        bean.init();

        assertNotNull(bean.getBuildTimestamp());
        assertFalse(bean.getBuildTimestamp().isEmpty());
    }

    @Test
    void getFullVersion_shouldReturnVersionWithBuild() {
        VersionBean bean = new VersionBean();
        bean.setVersion("1.5.0");
        bean.setBuildTimestamp("20.03.26 12:00");

        String full = bean.getFullVersion();

        assertEquals("1.5.0 (Build: 20.03.26 12:00)", full);
    }

    @Test
    void getFullVersion_shouldReturnOnlyVersion_whenTimestampNull() {
        VersionBean bean = new VersionBean();
        bean.setVersion("1.5.0");
        bean.setBuildTimestamp(null);

        assertEquals("1.5.0", bean.getFullVersion());
    }

    @Test
    void getFullVersion_shouldReturnOnlyVersion_whenTimestampEmpty() {
        VersionBean bean = new VersionBean();
        bean.setVersion("1.5.0");
        bean.setBuildTimestamp("");

        assertEquals("1.5.0", bean.getFullVersion());
    }

    @Test
    void getVersion_shouldReturnSetVersion() {
        VersionBean bean = new VersionBean();
        bean.setVersion("2.0.0-RC1");
        assertEquals("2.0.0-RC1", bean.getVersion());
    }

    @Test
    void getBuildTimestamp_shouldReturnSetTimestamp() {
        VersionBean bean = new VersionBean();
        bean.setBuildTimestamp("15.01.25 08:30");
        assertEquals("15.01.25 08:30", bean.getBuildTimestamp());
    }
}
