package io.sniffy.tls;

import io.sniffy.util.JVMUtil;
import io.sniffy.util.ReflectionUtil;

import java.lang.reflect.Method;

public class SniffyTlsModule {

    public static void initialize() {

        if (JVMUtil.getVersion() >= 16) {

            try {
                Class<?> moduleClass = Class.forName("java.lang.Module");
                Method implAddOpensMethod = moduleClass.getDeclaredMethod("implAddOpens", String.class);
                ReflectionUtil.setAccessible(implAddOpensMethod);

                Class<?> selChImplClass = Class.forName("sun.security.jca.Providers");
                Method getModuleMethod = Class.class.getMethod("getModule");

                Object module = getModuleMethod.invoke(selChImplClass);
                implAddOpensMethod.invoke(module, "sun.security.jca");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            SniffySecurityUtil.wrapJsseProvidersWithSniffy();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            SniffyProviderListUtil.install();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void uninstall() {

        try {
            SniffySecurityUtil.uninstall();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            SniffyProviderListUtil.uninstall();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
