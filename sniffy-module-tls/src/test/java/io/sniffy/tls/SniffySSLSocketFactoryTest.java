package io.sniffy.tls;

import io.qameta.allure.Issue;
import io.sniffy.reflection.field.FieldRef;
import io.sniffy.socket.BaseSocketTest;
import io.sniffy.util.JVMUtil;
import io.sniffy.util.ReflectionUtil;
import org.junit.Test;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.security.Security;
import java.util.Map;
import java.util.Properties;

import static io.sniffy.reflection.Unsafe.$;
import static org.junit.Assert.*;

public class SniffySSLSocketFactoryTest extends BaseSocketTest {

    @Test
    public void testFields() throws Exception {

        Map<String, FieldRef<SSLSocketFactory, ?>> fieldsMap = $(SSLSocketFactory.class).getDeclaredFields(false, true);

        assertTrue(fieldsMap.containsKey("theFactory"));
        assertTrue(fieldsMap.containsKey("propertyChecked"));
        assertTrue(fieldsMap.containsKey("DEBUG"));

        fieldsMap.remove("theFactory");
        fieldsMap.remove("propertyChecked");
        fieldsMap.remove("DEBUG");

        assertTrue(fieldsMap + " should be empty",fieldsMap.isEmpty());

    }

    public static class TestSSLSocketFactory extends SSLSocketFactory {

        @Override
        public String[] getDefaultCipherSuites() {
            return new String[0];
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return new String[0];
        }

        @Override
        public Socket createSocket(Socket socket, String s, int i, boolean b) throws IOException {
            return null;
        }

        @Override
        public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
            return null;
        }

        @Override
        public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
            return null;
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
            return null;
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
            return null;
        }

        @Override
        public Socket createSocket() throws IOException {
            return null;
        }

    }

    @Test
    @Issue("issues/439")
    public void testExistingSSLSocketFactoryWasCreateViaSecurityProperties() throws Exception {

        if (JVMUtil.getVersion() >= 13) {
            ReflectionUtil.setFields(
                    "javax.net.ssl.SSLSocketFactory$DefaultFactoryHolder",
                    null,
                    SSLSocketFactory.class,
                    new TestSSLSocketFactory()
            ); // cannot test "ssl.SocketFactory.provider" on Java 14+ since this property is used in static initializer
        } else {
            ReflectionUtil.setFields(SSLSocketFactory.class, null, SSLSocketFactory.class, null);
            ReflectionUtil.setFirstField(SSLSocketFactory.class, null, Boolean.TYPE, false);

            Security.setProperty("ssl.SocketFactory.provider", TestSSLSocketFactory.class.getName());
            SSLSocketFactory.getDefault();
        }

        assertEquals(TestSSLSocketFactory.class, SSLSocketFactory.getDefault().getClass());
        assertNull(SSLSocketFactory.getDefault().createSocket());

        try {
            SniffyTlsModule.initialize();

            {
                Socket socket = SSLSocketFactory.getDefault().createSocket(localhost, echoServerRule.getBoundPort());
                assertTrue(socket instanceof SniffySSLSocket);
            }

            SniffyTlsModule.uninstall();

            {
                Socket socket = SSLSocketFactory.getDefault().createSocket(localhost, echoServerRule.getBoundPort());
                assertFalse(socket instanceof SniffySSLSocket);
                assertNull(SSLSocketFactory.getDefault().createSocket());
            }

        } finally {

            if (JVMUtil.getVersion() >= 13) {
                ReflectionUtil.setFields(
                        "javax.net.ssl.SSLSocketFactory$DefaultFactoryHolder",
                        null,
                        SSLSocketFactory.class,
                        null
                );
            } else {
                ReflectionUtil.setFields(SSLSocketFactory.class, null, SSLSocketFactory.class, null);
                ReflectionUtil.setFirstField(SSLSocketFactory.class, null, Boolean.TYPE, false);

                Properties properties = ReflectionUtil.getFirstField(Security.class, null, Properties.class);
                if (null != properties) {
                    properties.remove("ssl.SocketFactory.provider");
                }
                SSLSocketFactory.getDefault();
            }

        }

    }

    @Test
    public void testCreateSocket() throws Exception {

        try {
            SniffyTlsModule.initialize();

            Socket socket = SSLSocketFactory.getDefault().createSocket(localhost, echoServerRule.getBoundPort());

            assertTrue(socket instanceof SniffySSLSocket);

        } finally {
            SniffyTlsModule.uninstall();

            Socket socket = SSLSocketFactory.getDefault().createSocket(localhost, echoServerRule.getBoundPort());

            assertFalse(socket instanceof SniffySSLSocket);
        }

    }

}