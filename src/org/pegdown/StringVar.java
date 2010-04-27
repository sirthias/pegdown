package org.pegdown;

import org.parboiled.Var;

public class StringVar extends Var<String> {

    public StringVar() {
    }

    public StringVar(String value) {
        super(value);
    }

    public boolean isEmpty() {
        return get() == null || get().length() == 0;
    }


    public boolean append(String text) {
        return set(get().concat(text));
    }

    public boolean append(String text1, String text2) {
        return set(get().concat(text1.concat(text2)));
    }

}
