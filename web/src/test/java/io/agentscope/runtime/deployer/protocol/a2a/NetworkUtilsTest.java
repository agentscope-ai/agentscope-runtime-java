package io.agentscope.runtime.deployer.protocol.a2a;

import io.agentscope.runtime.protocol.a2a.NetworkUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkUtilsTest {

    @Test
    @DisplayName("Test getServerIpAddress method")
    public void testGetServerIpAddress() {
        NetworkUtils networkUtils = new NetworkUtils(8080, null);


        String ipAddress = networkUtils.getServerIpAddress();

        assertNotNull(ipAddress);
        assertFalse(ipAddress.isEmpty());

        for (int i = 0 ; i < 100 ; i ++) {
            // The IP address should be consistent
            assertEquals(ipAddress, networkUtils.getServerIpAddress());
        }

    }
}
