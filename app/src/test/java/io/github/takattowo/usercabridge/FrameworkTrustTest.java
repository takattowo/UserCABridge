package io.github.takattowo.usercabridge;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class FrameworkTrustTest {
    // Android hidden classes are unavailable on the JVM; these fixtures model inspected AOSP shapes.
    public interface Source {
        Set<String> getCertificates();
    }

    public static class UserSource implements Source {
        private static final UserSource INSTANCE = new UserSource();

        public static UserSource getInstance() { return INSTANCE; }
        public Set<String> getCertificates() { return new HashSet<>(Arrays.asList("ca1", "ca2")); }
    }

    public static class BrokenSource implements Source {
        public static BrokenSource getInstance() { return new BrokenSource(); }
        public Set<String> getCertificates() { throw new IllegalStateException("store unreadable"); }
    }

    public static class Entry2 {
        private final Source mSource;
        final boolean overridesPins;

        public Entry2(Source source, boolean overridesPins) {
            mSource = source;
            this.overridesPins = overridesPins;
        }
    }

    public static class Entry3 {
        private final Source mSource;
        final boolean overridesPins;
        final boolean disableCT;

        public Entry3(Source source, boolean overridesPins, boolean disableCT) {
            mSource = source;
            this.overridesPins = overridesPins;
            this.disableCT = disableCT;
        }
    }

    public static class Entry4 {
        public Entry4(Source source, boolean first, boolean second, boolean unknown) { }
    }

    public static class Builder2 {
        final List<Entry2> refs = new ArrayList<>();
        final Object pins = new Object();
        final boolean cleartext = false;

        public Builder2 addCertificatesEntryRef(Entry2 ref) { refs.add(ref); return this; }
        List<Entry2> getCertificatesEntryRefs() { return refs; }
    }

    public static class Builder3 {
        final List<Entry3> refs = new ArrayList<>();
        final boolean requiresCT = true;

        public Builder3 addCertificatesEntryRef(Entry3 ref) { refs.add(ref); return this; }
        List<Entry3> getCertificatesEntryRefs() { return refs; }
    }

    public static class Config2 {
        public static Builder2 getDefaultBuilder(int sdk, int sandbox) { return new Builder2(); }
        public static Builder2 getDefaultBuilder(Object info) { return new Builder2(); }
        public Builder2 getDefaultBuilder() { return new Builder2(); }
    }

    public static class Config3 {
        public static Builder3 getDefaultBuilder(Object info) { return new Builder3(); }
    }

    public static class WrongConfig {
        public static Object getDefaultBuilder(Object info) { return new Object(); }
    }

    private FrameworkTrust legacy() throws ReflectiveOperationException {
        return FrameworkTrust.resolve(Config2.class, Builder2.class, UserSource.class,
                Source.class, Entry2.class, 35);
    }

    @Test
    public void appendsUserSourceAlongsideSystemWithoutChangingPinsOrCleartext() throws Exception {
        Builder2 builder = new Builder2();
        Entry2 system = new Entry2(HashSet::new, true);
        builder.refs.add(system);
        Object pins = builder.pins;

        assertTrue(legacy().addUserSource(builder));
        assertEquals(2, builder.refs.size());
        assertSame(system, builder.refs.get(0));
        assertTrue(system.overridesPins);
        assertSame(UserSource.getInstance(), builder.refs.get(1).mSource);
        assertFalse(builder.refs.get(1).overridesPins);
        assertSame(pins, builder.pins);
        assertFalse(builder.cleartext);
    }

    @Test
    public void android16UserSourceDoesNotOverridePinsOrDisableCertificateTransparency() throws Exception {
        FrameworkTrust trust = FrameworkTrust.resolve(Config3.class, Builder3.class,
                UserSource.class, Source.class, Entry3.class, 36);
        Builder3 builder = new Builder3();

        assertTrue(trust.addUserSource(builder));
        assertFalse(builder.refs.get(0).overridesPins);
        assertFalse(builder.refs.get(0).disableCT);
        assertTrue(builder.requiresCT);
    }

    @Test
    public void existingUserReferenceKeepsFlagsAndIsNotDuplicated() throws Exception {
        Builder2 builder = new Builder2();
        Entry2 existing = new Entry2(UserSource.getInstance(), true);
        builder.refs.add(existing);

        assertFalse(legacy().addUserSource(builder));
        assertEquals(1, builder.refs.size());
        assertSame(existing, builder.refs.get(0));
        assertTrue(existing.overridesPins);
    }

    @Test
    public void repeatedExtensionDoesNotDuplicateUserSource() throws Exception {
        FrameworkTrust trust = legacy();
        Builder2 builder = new Builder2();
        assertTrue(trust.addUserSource(builder));
        assertFalse(trust.addUserSource(builder));
        assertEquals(1, builder.refs.size());
    }

    @Test
    public void discoversFactoryOverloadsWithoutAssumingTheirParameters() throws Exception {
        assertEquals(2, legacy().getFactoryMethods().size());
    }

    @Test
    public void rejectsFactoryWithWrongReturnType() {
        assertThrows(ReflectiveOperationException.class, () -> FrameworkTrust.resolve(
                WrongConfig.class, Builder2.class, UserSource.class, Source.class, Entry2.class, 35));
    }

    @Test
    public void rejectsUnrecognizedConstructorRatherThanGuessingBooleanMeaning() {
        assertThrows(ReflectiveOperationException.class, () -> FrameworkTrust.resolve(
                Config2.class, Builder2.class, UserSource.class, Source.class, Entry4.class, 35));
    }

    @Test
    public void rejectsAndroid16ShapeOnOlderAndroid() {
        assertThrows(ReflectiveOperationException.class, () -> FrameworkTrust.resolve(
                Config3.class, Builder3.class, UserSource.class, Source.class, Entry3.class, 35));
    }

    @Test
    public void rejectsUninspectedAndroidVersion() {
        assertThrows(ReflectiveOperationException.class, () -> FrameworkTrust.resolve(
                Config2.class, Builder2.class, UserSource.class, Source.class, Entry2.class, 37));
    }

    @Test
    public void countsCertificatesFromPlatformUserSource() throws Exception {
        assertEquals(2, legacy().getUserCertificateCount());
    }

    @Test
    public void unreadableUserSourceDoesNotChangeOriginalBuilder() throws Exception {
        FrameworkTrust trust = FrameworkTrust.resolve(Config2.class, Builder2.class,
                BrokenSource.class, Source.class, Entry2.class, 35);
        Builder2 builder = new Builder2();
        Entry2 system = new Entry2(HashSet::new, false);
        builder.refs.add(system);
        assertThrows(InvocationTargetException.class, () -> trust.addUserSource(builder));
        assertSame(system, builder.refs.get(0));
        assertEquals(1, builder.refs.size());
    }
}
