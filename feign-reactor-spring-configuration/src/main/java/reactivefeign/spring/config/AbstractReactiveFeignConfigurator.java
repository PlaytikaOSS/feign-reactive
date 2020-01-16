package reactivefeign.spring.config;

abstract public class AbstractReactiveFeignConfigurator implements ReactiveFeignConfigurator {

    private final int order;

    protected AbstractReactiveFeignConfigurator(int order) {
        this.order = order;
    }

    @Override
    public int compareTo(ReactiveFeignConfigurator configurator){
        int compare = Integer.compare(order, ((AbstractReactiveFeignConfigurator) configurator).order);
        if(compare == 0){
            throw new IllegalArgumentException(String.format("Same order for different configurators: [%s], [%s]",
                    this, configurator));
        }
        return compare;
    }

}
