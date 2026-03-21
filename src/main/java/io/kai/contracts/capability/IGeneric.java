package io.kai.contracts.capability;

import java.util.List;
import java.util.Map;

public interface IGeneric {
    boolean addTypeParam();
    boolean addBoundedTypeParam(String name, String bound);
    Map<String, String> getTypeParams();
    boolean removeParam(String param);
    void clearParams();

    default String buildTypeParams() {
        Map<String, String> params = getTypeParams();
        if (params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("<");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) sb.append(", ");
            String bound = entry.getValue();
            if (bound.startsWith("reified ")) {
                String actualBound = bound.substring("reified ".length()).trim();
                sb.append("reified ").append(entry.getKey());
                if (!actualBound.isEmpty()) sb.append(" : ").append(actualBound);
            } else if (bound.equals("reified")) {
                sb.append("reified ").append(entry.getKey());
            } else if (!bound.isEmpty()) {
                sb.append(entry.getKey()).append(" : ").append(bound);
            } else {
                sb.append(entry.getKey());
            }
            first = false;
        }
        sb.append(">");
        return sb.toString();
    }
}
