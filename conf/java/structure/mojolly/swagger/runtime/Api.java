package mojolly.swagger.runtime;

import java.lang.Class;

public abstract class Api {
    private Client client = null;

    public Api(Client client) {
        this.client = client;
    }

    protected <T> T submit(String method, String path, Params params, Class<T> cls) {
        return client.submit(method, path,
               params.getQueryParams(),
               params.getPathParams(),
               params.getHeaderParams(),
               true, cls);
    }
}