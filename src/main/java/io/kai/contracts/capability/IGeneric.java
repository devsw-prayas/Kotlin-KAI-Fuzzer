package io.kai.contracts.capability;

import java.util.List;

public interface IGeneric {
    boolean addTypeParam();
    List<String> getTypeParams();
    boolean removeParam(String param);
    void clearParams();

    default String buildTypeParams(){
        List<String> list = getTypeParams();
        if(list.isEmpty()) return "";
        else return "<" + String.join(",", list) + ">";
    }
}
