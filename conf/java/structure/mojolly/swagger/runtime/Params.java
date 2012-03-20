package mojolly.swagger.runtime;

import java.util.*;

public abstract class Params {
    public class Param {
        public Param(String name, String value) {
            this.name = name;
            this.value = value;
        }
        public String name;
        public String value;
    }

    public abstract List<Param> getHeaderParams();
    public abstract List<Param> getQueryParams();
    public abstract List<Param> getPathParams();
}