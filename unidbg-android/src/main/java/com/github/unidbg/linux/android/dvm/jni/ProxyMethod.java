package com.github.unidbg.linux.android.dvm.jni;

import com.github.unidbg.linux.android.dvm.VM;
import unicorn.UnicornException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ProxyMethod implements ProxyCall {

    private final Method method;
    private final Object[] args;

    ProxyMethod(Method method, Object[] args) {
        this.method = method;
        this.args = args;
    }

    @Override
    public Object call(VM vm, Object obj) throws IllegalAccessException, InvocationTargetException {
        try {
            patch(obj, args);

            return method.invoke(obj, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof UnicornException) {
                throw (UnicornException) cause;
            }
            if (cause instanceof ProxyDvmException) {
                vm.throwException(ProxyDvmObject.createObject(vm, cause));
                return null;
            }
            if (cause instanceof ClassNotFoundException) {
                vm.throwException(ProxyDvmObject.createObject(vm, cause));
                return null;
            }
            throw e;
        }
    }

    private void patch(Object obj, Object[] args) {
        if (obj instanceof ClassLoader &&
                args.length == 1 &&
                ("loadClass".equals(method.getName()) || "findClass".equals(method.getName()))) {
            String binaryName = (String) args[0];
            args[0] = binaryName.replace('/', '.');
        }
    }
}
