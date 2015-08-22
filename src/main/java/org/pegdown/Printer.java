/*
 * Copyright (C) 2010-2011 Mathias Doenitz
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

/**
 * Encapsulates basic string output functionality.
 */
public class Printer {
    public final StringBuilder sb;
    public int indent;

    public Printer() {
        this(new StringBuilder());
    }

    public Printer(StringBuilder sb) {
        this.sb = sb;
    }

    public Printer indent(int delta) {
        indent += delta;
        return this;
    }

    public Printer print(String string) {
        sb.append(string);
        return this;
    }

    public Printer printEncoded(String string) {
        FastEncoder.encode(string, sb);
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

    public Printer printchkln() {
        if (!endsWithNewLine()) {
            if (sb.length() > 0) print('\n');
            for (int i = 0; i < indent; i++) print(' ');
        }
        return this;
    }

    public Printer printchkln(boolean printNewLine) {
        if (printNewLine) {
            if (sb.length() > 0) print('\n');
            for (int i = 0; i < indent; i++) print(' ');
        }
        return this;
    }

    public boolean endsWithNewLine() {
        int iMax = sb.length();

        for (int i = iMax; i-- > 0; ) {
           if (sb.charAt(i) != ' ') {
               return sb.charAt(i) == '\n';
           }
        }
        // all leading spaces
        return false;
    }

    public String getString() {
        return sb.toString();
    }
    
    public Printer clear() {
        sb.setLength(0);
        return this;
    }
}
