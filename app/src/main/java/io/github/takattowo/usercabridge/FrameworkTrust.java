package io.github.takattowo.usercabridge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

final class FrameworkTrust {
    private final List<Method> factories;
    private final Constructor<?> referenceConstructor;
    private final Method addReference;
    private final Method getReferences;
    private final Method getCertificates;
    private final Field sourceField;
    private final Object userSource;

    private FrameworkTrust(List<Method> factories, Constructor<?> referenceConstructor,
            Method addReference, Method getReferences, Method getCertificates,
            Field sourceField, Object userSource) {
        this.factories = factories;
        this.referenceConstructor = referenceConstructor;
        this.addReference = addReference;
        this.getReferences = getReferences;
        this.getCertificates = getCertificates;
        this.sourceField = sourceField;
        this.userSource = userSource;
    }

    static FrameworkTrust resolve(Class<?> config, Class<?> builder, Class<?> user,
            Class<?> source, Class<?> entry, int sdk) throws ReflectiveOperationException {
        if (sdk < 26 || sdk > 36) {
            throw new ReflectiveOperationException("Uninspected Android API " + sdk);
        }
        List<Method> factories = new ArrayList<>();
        for (Method method : config.getDeclaredMethods()) {
            if (method.getName().equals("getDefaultBuilder")
                    && Modifier.isStatic(method.getModifiers())
                    && method.getReturnType() == builder) {
                factories.add(method);
            }
        }
        if (factories.isEmpty()) {
            throw new NoSuchMethodException("No supported getDefaultBuilder in " + config.getName());
        }

        Constructor<?> constructor = null;
        for (Constructor<?> candidate : entry.getDeclaredConstructors()) {
            Class<?>[] parameters = candidate.getParameterTypes();
            // AOSP 8–15: source/overridePins. AOSP 16 adds disableCT; never guess extra flags.
            if (parameters.length == (sdk == 36 ? 3 : 2)
                    && parameters[0] == source && parameters[1] == boolean.class
                    && (parameters.length == 2 || parameters[2] == boolean.class)) {
                if (constructor != null) {
                    throw new NoSuchMethodException("Ambiguous certificate-reference constructor");
                }
                constructor = candidate;
            }
        }
        if (constructor == null) {
            throw new NoSuchMethodException("Unsupported " + entry.getName()
                    + " constructors: " + Arrays.toString(entry.getDeclaredConstructors()));
        }
        constructor.setAccessible(true);
        Method addReference = findMethod(builder, "addCertificatesEntryRef", builder, false, entry);
        Method getReferences = findMethod(builder, "getCertificatesEntryRefs", List.class, false);
        Method getCertificates = findMethod(source, "getCertificates", Set.class, false);
        Method getInstance = findMethod(user, "getInstance", user, true);
        Field sourceField = entry.getDeclaredField("mSource");
        if (sourceField.getType() != source) {
            throw new NoSuchFieldException("Unexpected certificate-reference source type");
        }
        sourceField.setAccessible(true);
        Object userSource = getInstance.invoke(null);
        if (!source.isInstance(userSource) || !user.isInstance(userSource)) {
            throw new ReflectiveOperationException("Invalid user certificate source singleton");
        }
        return new FrameworkTrust(factories, constructor, addReference, getReferences,
                getCertificates, sourceField, userSource);
    }

    private static Method findMethod(Class<?> owner, String name, Class<?> returnType,
            boolean isStatic, Class<?>... parameters) throws ReflectiveOperationException {
        Method found = null;
        for (Method candidate : owner.getDeclaredMethods()) {
            if (candidate.getName().equals(name)
                    && Modifier.isStatic(candidate.getModifiers()) == isStatic
                    && returnType.isAssignableFrom(candidate.getReturnType())
                    && Arrays.equals(candidate.getParameterTypes(), parameters)) {
                if (found != null) {
                    throw new NoSuchMethodException("Ambiguous " + owner.getName() + "." + name);
                }
                found = candidate;
            }
        }
        if (found == null) {
            throw new NoSuchMethodException("Unsupported " + owner.getName() + "." + name);
        }
        found.setAccessible(true);
        return found;
    }

    List<Method> getFactoryMethods() { return factories; }
    Constructor<?> getReferenceConstructor() { return referenceConstructor; }

    boolean addUserSource(Object builder) throws ReflectiveOperationException {
        List<?> references = (List<?>) getReferences.invoke(builder);
        if (references != null) {
            for (Object reference : references) {
                if (sourceField.get(reference) == userSource) {
                    return false;
                }
            }
        }
        // Reject an unreadable new source before it can break later framework trust loading.
        getUserCertificateCount();
        // false preserves pins and, on Android 16, existing CT requirements.
        Object reference = referenceConstructor.getParameterCount() == 2
                ? referenceConstructor.newInstance(userSource, false)
                : referenceConstructor.newInstance(userSource, false, false);
        addReference.invoke(builder, reference);
        return true;
    }

    int getUserCertificateCount() throws ReflectiveOperationException {
        return ((Set<?>) getCertificates.invoke(userSource)).size();
    }
}
