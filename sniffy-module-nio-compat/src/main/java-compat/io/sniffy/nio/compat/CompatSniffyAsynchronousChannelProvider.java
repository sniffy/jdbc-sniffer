package io.sniffy.nio.compat;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.reflection.UnsafeException;
import io.sniffy.reflection.field.FieldRef;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import static io.sniffy.reflection.Unsafe.$;

// TODO: this functionality is available in java 1.7+ only - make sure it is safe
// TODO: integrate with Sniffy; currently it is not used
/**
 * @since 3.1.7
 */
public class CompatSniffyAsynchronousChannelProvider extends AsynchronousChannelProvider {

    private static final Polyglog LOG = PolyglogFactory.log(CompatSniffyAsynchronousChannelProvider.class);

    private static volatile AsynchronousChannelProvider previousAsynchronousSelectorProvider;

    private final AsynchronousChannelProvider delegate;

    public CompatSniffyAsynchronousChannelProvider(AsynchronousChannelProvider delegate) {
        this.delegate = delegate;
    }

    public static boolean install() {
        AsynchronousChannelProvider delegate = AsynchronousChannelProvider.provider();

        LOG.info("Original AsynchronousChannelProvider was " + delegate);

        if (null != delegate && CompatSniffyAsynchronousChannelProvider.class.equals(delegate.getClass())) {
            return true;
        }

        if (null == previousAsynchronousSelectorProvider && !CompatSniffyAsynchronousChannelProvider.class.equals(delegate.getClass())) {
            previousAsynchronousSelectorProvider = delegate;
        }

        CompatSniffyAsynchronousChannelProvider sniffyAsynchronousSelectorProvider = new CompatSniffyAsynchronousChannelProvider(delegate);

        try {
            FieldRef<Object, Object> instanceFieldRef = $("java.nio.channels.spi.AsynchronousChannelProvider$ProviderHolder").field("provider");
            if (instanceFieldRef.isResolved()) {
                instanceFieldRef.set(null, sniffyAsynchronousSelectorProvider);
                return true;
            } else {
                LOG.error("Couldn't initialize SniffyAsynchronousChannelProvider since java.nio.channels.spi.AsynchronousChannelProvider$ProviderHolder.provider is unavailable");
                return false;
            }
        } catch (UnsafeException e) {
            LOG.error(e);
            return false;
        }

    }

    public static boolean uninstall() {

        LOG.info("Restoring original SelectorProvider " + previousAsynchronousSelectorProvider);

        if (null == previousAsynchronousSelectorProvider) {
            return false;
        }

        try {
            FieldRef<Object, Object> instanceFieldRef = $("java.nio.channels.spi.AsynchronousChannelProvider$ProviderHolder").field("provider");
            if (instanceFieldRef.isResolved()) {
                instanceFieldRef.set(null, previousAsynchronousSelectorProvider);
                return true;
            } else {
                LOG.error("Couldn't initialize SniffyAsynchronousChannelProvider since java.nio.channels.spi.AsynchronousChannelProvider$ProviderHolder.provider is unavailable");
                return false;
            }
        } catch (UnsafeException e) {
            LOG.error(e);
            return false;
        }

    }

    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(int nThreads, ThreadFactory threadFactory) throws IOException {
        return delegate.openAsynchronousChannelGroup(nThreads, threadFactory);
    }

    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(ExecutorService executor, int initialSize) throws IOException {
        return delegate.openAsynchronousChannelGroup(executor, initialSize);
    }

    @Override
    public AsynchronousServerSocketChannel openAsynchronousServerSocketChannel(AsynchronousChannelGroup group) throws IOException {
        return delegate.openAsynchronousServerSocketChannel(group);
    }

    @Override
    public AsynchronousSocketChannel openAsynchronousSocketChannel(AsynchronousChannelGroup group) throws IOException {
        return new CompatSniffyAsynchronousSocketChannel(this, delegate.openAsynchronousSocketChannel(group));
    }
}
