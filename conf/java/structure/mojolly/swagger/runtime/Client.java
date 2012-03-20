package mojolly.swagger.runtime;

import java.lang.String;

public class Client {
    private ApiAuth auth = null;

    public Client(ApiAuth auth) {
        this.auth = auth;
    }

    public <T> T submit(String method, String path, Params params) {
        return null;
    }
}