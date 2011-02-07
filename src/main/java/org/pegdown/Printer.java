/*
 * Copyright (C) 2010 Mathias Doenitz
 *
 * Based on peg-markdown (C) 2008-2010 John MacFarlane
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pegdown;

import org.parboiled.common.StringUtils;

import java.util.LinkedList;

/**
 * Encapsulates basic string output functionality.
 */
public class Printer {

    public interface Encoder {

        /**
         * Returns a string representation for the given character or null, if the character does not have to be encoded.
         *
         * @param c the character
         * @return a string or null
         */
        String encode(char c);
    }

    private final StringBuilder sb = new StringBuilder();
    private int indent;
    private LinkedList<Encoder> priorEncoders;
    private Encoder encoder;

    public Printer indent(int delta) {
        indent += delta;
        return this;
    }

    public Printer startEncoding(Encoder encoder) {
        if (this.encoder != null) {
            if (priorEncoders == null) priorEncoders = new LinkedList<Encoder>();
            priorEncoders.addFirst(this.encoder);
        }
        this.encoder = encoder;
        return this;
    }

    public Printer stopEncoding() {
        encoder = priorEncoders == null || priorEncoders.isEmpty() ? null : priorEncoders.removeFirst();
        return this;
    }
    
    public Printer printEncoded(String string, Encoder encoder) {
        return startEncoding(encoder).print(string).stopEncoding();
    }
    
    public Printer printEncoded(char c, Encoder encoder) {
        return startEncoding(encoder).print(c).stopEncoding();
    }

    public Printer print(String string) {
        if (StringUtils.isNotEmpty(string)) {
            if (encoder != null) {
                for (int i = 0; i < string.length(); i++) {
                    if (encoder.encode(string.charAt(i)) != null) {
                        // we have at least one character that needs encoding, so do it one by one
                        for (i = 0; i < string.length(); i++) {
                            char c = string.charAt(i);
                            String encoded = encoder.encode(c);
                            if (encoded != null) {
                                sb.append(encoded);
                            } else {
                                sb.append(c);
                            }
                        }
                        return this;
                    }
                }
            }
            sb.append(string);
        }
        return this;
    }

    public Printer print(char c) {
        sb.append(c);
        return this;
    }

    public Printer println() {
        if (sb.length() > 0) print('\n');
        for (int i = 0; i < indent; i++) print(' ');
        return this;
    }

    public String getString() {
        return sb.toString();
    }
    
    public Printer clear() {
        sb.setLength(0);
        return this;
    }
}
