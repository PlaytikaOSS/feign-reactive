package reactivefeign.spring.config;

abstract public class AbstractReactiveFeignConfigurator implements ReactiveFeignConfigurator {

    private final int order;

    protected AbstractReactiveFeignConfigurator(int order) {
        this.order = order;
    }

    @Override
    public int compareTo(ReactiveFeignConfigurator configurator){
        if (this == configurator) {
            return 0;
        }
        int compare = Integer.compare(order, ((AbstractReactiveFeignConfigurator) configurator).order);
        if(compare == 0){
            throw new IllegalArgumentException("Same order for different configurators: [%s], [%s]".formatted(
                    this, configurator));
        }
        return compare;
    }

}
