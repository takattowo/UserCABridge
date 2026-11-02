package io.github.takattowo.usercabridge;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

// Test-first remote baseline; replaced after observing failing behavioral checks.
final class FrameworkTrust {
    static FrameworkTrust resolve(Class<?> config, Class<?> builder, Class<?> user,
            Class<?> source, Class<?> entry, int sdk) throws ReflectiveOperationException {
        return new FrameworkTrust();
    }

    List<Method> getFactoryMethods() { return Collections.emptyList(); }
    boolean addUserSource(Object builder) throws ReflectiveOperationException { return false; }
    int getUserCertificateCount() throws ReflectiveOperationException { return 0; }
}
