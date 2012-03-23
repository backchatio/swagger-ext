package mojolly.swagger.runtime;

import java.util.*;

public class Client extends JvmClient {

    public Client(ClientConfig config) {
        super(config);
    }

    public <T> T submit(String method, String path, Params params, Boolean authRequired, Class<T> cls) {
        printParams("Headers", params.getHeaderParams());
        printParams("Path", params.getPathParams());
        printParams("Query", params.getQueryParams());

        return super.submit(method, path, params.getQueryParams(), params.getPathParams(), params.getHeaderParams(), authRequired, cls);
    }

    public void printParams(String name, List<Param> params) {
        System.out.print(name + ": ");
        for (Param header : params)
            System.out.print(String.format("%s: %s", header.getKey(), header.getValue()));
        System.out.println("");
    }
}