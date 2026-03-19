/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlaintextIncomingEmailListenerTest {

    @Test
    void getConfigNamesToListenTo_defaultIsNull() {
        PlaintextIncomingEmailListener listener = new PlaintextIncomingEmailListener() {
            @Override
            public void onEmailReceived(Long emailId, String mandat, String configName) {}

            @Override
            public String getListenerName() {
                return "TestListener";
            }
        };

        assertNull(listener.getConfigNamesToListenTo());
    }

    @Test
    void getConfigNamesToListenTo_canBeOverridden() {
        PlaintextIncomingEmailListener listener = new PlaintextIncomingEmailListener() {
            @Override
            public void onEmailReceived(Long emailId, String mandat, String configName) {}

            @Override
            public String getListenerName() {
                return "FilteredListener";
            }

            @Override
            public List<String> getConfigNamesToListenTo() {
                return List.of("support", "info");
            }
        };

        List<String> configNames = listener.getConfigNamesToListenTo();
        assertNotNull(configNames);
        assertEquals(2, configNames.size());
        assertTrue(configNames.contains("support"));
        assertTrue(configNames.contains("info"));
    }
}
