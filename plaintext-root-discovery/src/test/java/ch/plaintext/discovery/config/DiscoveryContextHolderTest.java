/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscoveryContextHolderTest {

    @Test
    void setAndGetApplicationContext() {
        ApplicationContext mockContext = mock(ApplicationContext.class);

        DiscoveryContextHolder holder = new DiscoveryContextHolder();
        holder.setApplicationContext(mockContext);

        assertEquals(mockContext, DiscoveryContextHolder.getContext());
    }

    @Test
    void getBeanDelegatesToContext() {
        ApplicationContext mockContext = mock(ApplicationContext.class);
        String expectedBean = "test-bean";
        when(mockContext.getBean(String.class)).thenReturn(expectedBean);

        DiscoveryContextHolder holder = new DiscoveryContextHolder();
        holder.setApplicationContext(mockContext);

        String result = DiscoveryContextHolder.getBean(String.class);
        assertEquals("test-bean", result);
        verify(mockContext).getBean(String.class);
    }

    @Test
    void getBeanReturnsNullWhenContextIsNull() {
        // Set context to null
        DiscoveryContextHolder holder = new DiscoveryContextHolder();
        holder.setApplicationContext(null);

        assertNull(DiscoveryContextHolder.getBean(String.class));
    }
}
