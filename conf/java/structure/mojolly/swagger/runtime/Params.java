package mojolly.swagger.runtime;

import java.lang.Integer;
import java.lang.String;
import java.util.*;

public abstract class Params {

    public abstract List<Param> getHeaderParams();
    public abstract List<Param> getQueryParams();
    public abstract List<Param> getPathParams();

    public <T> Param createParam(String name, T value) {
        String s = null;
        if (value.getClass() == String.class)
            s = value.toString();
        else if (value.getClass() == Integer.class)
            s = Integer.toString((Integer) value);
        else if (value.getClass() == Boolean.class)
            s = Boolean.toString((Boolean) value);
        if (s == null)
            return null;
        else
            return new Param(name, value.toString());
    }

    public <T> List<Param> createParams(String name, T value) {
        List<Param> params = new ArrayList<Param>();
        if (value != null) {
            Param param = createParam(name, value);
            if (param != null)
                params.add(param);
        }
        return params;
    }

    public <T> List<Param> createParams(String name, List<T> values) {
        List<Param> params = new ArrayList<Param>();
        if (values != null)
            for (T value : values) {
                Param param = createParam(name, value);
                if (param != null)
                    params.add(param);
            }
        return params;
    }
}