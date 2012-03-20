package mojolly.swagger.runtime;

public abstract class Api {
    private Client client = null;

    public Api(Client client) {
        this.client = client;
    }

    protected <T> T submit(String method, String path, Params params) {
        return client.submit(method, path, params);
    }
}