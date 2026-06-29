package reactivefeign;

import org.junit.jupiter.api.Test;
import reactivefeign.methodhandler.DefaultMethodHandler;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultMethodHandlerTest extends BaseReactorTest {

    @Test
    public void shouldThrowErrorOnNotDefaultMethod() throws NoSuchMethodException {
      assertThrows(AbstractMethodError.class, () -> {
        new DefaultMethodHandler(TestInterface.class.getMethod("notDefaultMethod"));
      });
    }

    @Test
    public void shouldFailIfNotBoundToProxy() throws Throwable {
      assertThrows(IllegalStateException.class, () -> {
        DefaultMethodHandler defaultMethodHandler
                = new DefaultMethodHandler(TestInterface.class.getMethod("defaultMethod"));
        defaultMethodHandler.invoke(new Object[0]);
      });
    }

    @Test
    public void shouldFailOnRebind() throws Throwable {
      assertThrows(IllegalStateException.class, () -> {
        DefaultMethodHandler defaultMethodHandler
                = new DefaultMethodHandler(TestInterface.class.getMethod("defaultMethod"));

        TestInterface mockImplementation = mock(TestInterface.class);
        defaultMethodHandler.bindTo(mockImplementation);
        defaultMethodHandler.bindTo(mockImplementation);
      });
    }

    @Test
    public void shouldCallNotDefaultMethodOnActualImplementation() throws Throwable {
        DefaultMethodHandler defaultMethodHandler
                = new DefaultMethodHandler(TestInterface.class.getMethod("defaultMethod"));

        TestInterface mockImplementation = mock(TestInterface.class);
        when(mockImplementation.defaultMethod()).thenCallRealMethod();

        defaultMethodHandler.bindTo(mockImplementation);

        defaultMethodHandler.invoke(new Object[0]);

        verify(mockImplementation).notDefaultMethod();
    }

    interface TestInterface {
        Mono<String> notDefaultMethod();

        default Mono<String> defaultMethod(){
            return notDefaultMethod();
        }
    }

}
