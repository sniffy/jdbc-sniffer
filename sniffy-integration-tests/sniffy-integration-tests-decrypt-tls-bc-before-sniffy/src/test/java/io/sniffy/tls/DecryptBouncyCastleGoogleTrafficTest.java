package io.sniffy.tls;

import io.sniffy.*;
import io.sniffy.socket.AddressMatchers;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.test.junit.SniffyRunner;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.net.ssl.SSLContext;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.security.Security;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SniffyRunner.class)
public class DecryptBouncyCastleGoogleTrafficTest {

    @SuppressWarnings("CharsetObjectCanBeUsed")
    @Test
    public void testGoogleTraffic() throws Exception {

        assertTrue(SSLContext.getInstance("Default").getProvider().getName().contains("Sniffy"));

        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        Security.insertProviderAt(new BouncyCastleJsseProvider(), 1);

        // Sniffy.reinitialize(); // https://github.com/sniffy/sniffy/issues/478 - bug in io.sniffy.tls.SniffyProviderListUtil

        SSLContext instance = SSLContext.getInstance("TLSv1", "BCJSSE");
        instance.init(null, null, new SecureRandom());
        assertTrue(instance.getSocketFactory() instanceof SniffySSLSocketFactory);

        assertEquals("Sniffy-BCJSSE", SSLContext.getDefault().getProvider().getName());

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).captureStackTraces(true).build())) {

            URL url = new URL("https://www.google.com");
            URLConnection urlConnection = url.openConnection();

            //noinspection ResultOfMethodCallIgnored
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

            assertTrue(new String(request.getBytes(), Charset.forName("US-ASCII")).contains("Host: www.google.com"));
            assertTrue(new String(response.getBytes(), Charset.forName("US-ASCII")).contains("200"));

        }

    }

}
