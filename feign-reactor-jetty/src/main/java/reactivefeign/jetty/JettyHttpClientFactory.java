package reactivefeign.jetty;

import org.eclipse.jetty.client.HttpClient;

public interface JettyHttpClientFactory {

    HttpClient build(boolean useHttp2);

}
