package mojolly.swagger.runtime;

import java.util.Map;

public interface ApiAuth {
    Map<String, String> getHeaders();
}