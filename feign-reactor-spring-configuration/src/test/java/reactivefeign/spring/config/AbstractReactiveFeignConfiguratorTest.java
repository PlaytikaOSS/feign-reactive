package reactivefeign.spring.config;

import org.junit.Test;
import reactivefeign.ReactiveFeignBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AbstractReactiveFeignConfiguratorTest {

    @Test
    public void shouldCompareSameInstance() {
        TestConfigurator configurator = new TestConfigurator(1);
        assertThat(configurator.compareTo(configurator)).isEqualTo(0);
    }

    @Test
    public void shouldCompareDifferentOrder() {
        TestConfigurator configurator1 = new TestConfigurator(1);
        TestConfigurator configurator2 = new TestConfigurator(2);

        assertThat(configurator1.compareTo(configurator2)).isLessThan(0);
        assertThat(configurator2.compareTo(configurator1)).isGreaterThan(0);
    }

    @Test
    public void shouldThrowExceptionWhenSameOrder() {
        TestConfigurator configurator1 = new TestConfigurator(1);
        TestConfigurator configurator2 = new TestConfigurator(1);

        assertThatThrownBy(() -> configurator1.compareTo(configurator2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Same order for different configurators");
    }

    private static class TestConfigurator extends AbstractReactiveFeignConfigurator {
        protected TestConfigurator(int order) {
            super(order);
        }

        @Override
        public ReactiveFeignBuilder configure(ReactiveFeignBuilder builder, ReactiveFeignNamedContext namedContext) {
            return builder;
        }
    }
}
