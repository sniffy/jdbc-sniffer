package io.sniffy.proxy;

import io.sniffy.*;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.AddressMatchers;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.SocketMetaData;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ConnectionViaHttpProxyTest {

    @Rule
    public ProxyServerRule rule = new ProxyServerRule();

    @BeforeClass
    public static void loadTlsModule() {
        SniffyConfiguration.INSTANCE.setDecryptTls(true);
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        Sniffy.initialize();
    }

    @Test
    public void testGoogleTraffic() throws Exception {

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).captureStackTraces(true).build())) {

            URL url = new URL("https://www.google.com");
            URLConnection urlConnection = url.openConnection(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("localhost", 8080)));

            urlConnection.getInputStream().read();

            Map<SocketMetaData, List<NetworkPacket>> decryptedNetworkTraffic = spy.getDecryptedNetworkTraffic(
                    Threads.CURRENT,
                    AddressMatchers.exactAddressMatcher("www.google.com:443"),
                    GroupingOptions.builder().
                            groupByConnection(false).
                            groupByStackTrace(false).
                            groupByThread(false).
                            build()
            );

            assertEquals(1, decryptedNetworkTraffic.size());

            Map.Entry<SocketMetaData, List<NetworkPacket>> entry = decryptedNetworkTraffic.entrySet().iterator().next();

            assertNotNull(entry);
            assertNotNull(entry.getKey());
            assertNotNull(entry.getValue());

            assertEquals(2, entry.getValue().size());

            NetworkPacket request = entry.getValue().get(0);
            NetworkPacket response = entry.getValue().get(1);

            //noinspection SimplifiableAssertion
            assertEquals(true, request.isSent());
            //noinspection SimplifiableAssertion
            assertEquals(false, response.isSent());

            //noinspection CharsetObjectCanBeUsed
            assertTrue(new String(request.getBytes(), Charset.forName("US-ASCII")).contains("Host: www.google.com"));
            //noinspection CharsetObjectCanBeUsed
            String responseBody = new String(response.getBytes(), Charset.forName("US-ASCII"));
            assertTrue(responseBody.startsWith("HTTP"));
            assertTrue(responseBody.contains("200"));

        }

        Map<Map.Entry<String, Integer>, Integer> discoveredAddresses = ConnectionsRegistry.INSTANCE.getDiscoveredAddresses();

        assertTrue(discoveredAddresses.containsKey(new AbstractMap.SimpleEntry<>("www.google.com", 443)));
        assertTrue(discoveredAddresses.containsKey(new AbstractMap.SimpleEntry<>("localhost", 8080)));

    }

}
