package reactivefeign;

import org.junit.Test;
import reactivefeign.methodhandler.DefaultMethodHandler;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultMethodHandlerTest extends BaseReactorTest {

    @Test(expected = AbstractMethodError.class)
    public void shouldThrowErrorOnNotDefaultMethod() throws NoSuchMethodException {
        new DefaultMethodHandler(TestInterface.class.getMethod("notDefaultMethod"));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailIfNotBoundToProxy() throws Throwable {
        DefaultMethodHandler defaultMethodHandler
                = new DefaultMethodHandler(TestInterface.class.getMethod("defaultMethod"));
        defaultMethodHandler.invoke(new Object[0]);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailOnRebind() throws Throwable {
        DefaultMethodHandler defaultMethodHandler
                = new DefaultMethodHandler(TestInterface.class.getMethod("defaultMethod"));

        TestInterface mockImplementation = mock(TestInterface.class);
        defaultMethodHandler.bindTo(mockImplementation);
        defaultMethodHandler.bindTo(mockImplementation);
    }

    @Test
    public void shouldCallNotDefaultMethodOnActualImplementation() throws Throwable {
        DefaultMethodHandler defaultMethodHandler
                = new DefaultMethodHandler(TestInterface.class.getMethod("defaultMethod"));

        TestInterface mockImplementation = mock(TestInterface.class);

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
