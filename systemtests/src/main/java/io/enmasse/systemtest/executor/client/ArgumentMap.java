package io.enmasse.systemtest.executor.client;

import java.util.*;

/**
 * Class represents Map of arguments (allow duplicate argument)
 */
public class ArgumentMap {
    private final Map<Argument,ArrayList<String>> mappings = new HashMap<>();

    /**
     * Returns set of values for argument
     * @param arg argument
     * @return Set of values
     */
    public ArrayList<String> getValues(Argument arg)
    {
        return mappings.get(arg);
    }

    /**
     * Returns set of arguments
     * @return set of arguments
     */
    public Set<Argument> getArguments(){
        return mappings.keySet();
    }

    /**
     * Removes argument from map
     * @param key name of argument
     */
    public void remove(Argument key){
        mappings.remove(key);
    }

    /**
     * Add argument and his values
     * @param key arguments
     * @param value value
     * @return true if operation is completed
     */
    public Boolean put(Argument key, String value) {
        ArrayList<String> target = mappings.get(key);

        if(target == null) {
            target = new ArrayList<>();
            mappings.put(key,target);
        }

        return target.add(value);
    }
}