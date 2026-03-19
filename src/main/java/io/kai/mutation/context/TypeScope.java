package io.kai.mutation.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TypeScope {
    private final List<String> params = new ArrayList<>();


    public void declare(String type){
        if(!params.contains(type)) params.add(type);
    }

    public List<String> getParams(){
        return Collections.unmodifiableList(params);
    }
}
