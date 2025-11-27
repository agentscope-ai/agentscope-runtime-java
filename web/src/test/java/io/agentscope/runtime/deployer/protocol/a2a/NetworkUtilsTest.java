package io.agentscope.runtime.deployer.protocol.a2a;

import io.agentscope.runtime.protocol.a2a.NetworkUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.MethodOrderer;


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
