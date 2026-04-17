/*
 * JUnit 5 extension to manage WireMockServer lifecycle for tests that still use @Rule WireMockClassRule.
 * Since JUnit 4 @Rule is ignored by JUnit 5, this extension discovers fields of type WireMockServer
 * (including subclasses like WireMockClassRule) via reflection and starts/stops them appropriately.
 *
 * Static fields: started in beforeAll, reset in beforeEach, stopped in afterAll.
 * Instance fields: started in beforeEach, stopped in afterEach.
 */
package reactivefeign.testcase;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class WireMockLifecycleExtension implements BeforeAllCallback, BeforeEachCallback,
        AfterEachCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        for (Field field : collectWireMockFields(testClass, true)) {
            field.setAccessible(true);
            WireMockServer server = (WireMockServer) field.get(null);
            if (server != null && !server.isRunning()) {
                server.start();
            }
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Object testInstance = context.getRequiredTestInstance();
        Class<?> testClass = context.getRequiredTestClass();

        // Start/reset static
        for (Field field : collectWireMockFields(testClass, true)) {
            field.setAccessible(true);
            WireMockServer server = (WireMockServer) field.get(null);
            if (server != null) {
                if (!server.isRunning()) {
                    server.start();
                }
                server.resetAll();
            }
        }

        // Start instance
        for (Field field : collectWireMockFields(testClass, false)) {
            field.setAccessible(true);
            WireMockServer server = (WireMockServer) field.get(testInstance);
            if (server != null && !server.isRunning()) {
                server.start();
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (context.getTestInstance().isEmpty()) {
            return;
        }
        Object testInstance = context.getRequiredTestInstance();
        Class<?> testClass = context.getRequiredTestClass();
        for (Field field : collectWireMockFields(testClass, false)) {
            field.setAccessible(true);
            WireMockServer server = (WireMockServer) field.get(testInstance);
            if (server != null && server.isRunning()) {
                server.stop();
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        for (Field field : collectWireMockFields(testClass, true)) {
            field.setAccessible(true);
            WireMockServer server = (WireMockServer) field.get(null);
            if (server != null && server.isRunning()) {
                server.stop();
            }
        }
    }

    private List<Field> collectWireMockFields(Class<?> testClass, boolean staticOnly) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = testClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                boolean isStatic = Modifier.isStatic(field.getModifiers());
                if (staticOnly != isStatic) {
                    continue;
                }
                if (WireMockServer.class.isAssignableFrom(field.getType())) {
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }
}
