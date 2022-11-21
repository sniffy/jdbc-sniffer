package io.sniffy.reflection.method;

import io.sniffy.reflection.UnsafeException;
import io.sniffy.util.ExceptionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NonVoidZeroArgsMethodRef<T, C> extends AbstractMethodRef<C> {

    public NonVoidZeroArgsMethodRef(Method method, Throwable throwable) {
        super(method, throwable);
    }

    public T invoke(C instance) throws UnsafeException {
        try {
            if (null != throwable) {
                throw ExceptionUtil.throwException(throwable);
            } else {
                Object result = method.invoke(instance);
                //noinspection unchecked
                return (T) result;
            }
        } catch (IllegalAccessException e) {
            throw new UnsafeException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.throwException(e.getTargetException());
        }
    }

}
