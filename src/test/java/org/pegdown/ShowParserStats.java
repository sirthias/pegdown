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

import org.parboiled.Parboiled;
import org.parboiled.ParserStatistics;
import org.parboiled.Rule;

public class ShowParserStats {

    public static void main(String[] args) {
        System.out.print("Creating processor... :");
        long start = System.currentTimeMillis();
        PegDownProcessor processor = new PegDownProcessor();
        time(start);

        Parser parser = Parboiled.createParser(Parser.class, Extensions.NONE);
        ParserStatistics stats = ParserStatistics.generateFor(parser.Root());
        System.out.println(stats);
        System.out.println(stats.printActionClassInstances());
    }

    private static long time(long start) {
        long end = System.currentTimeMillis();
        System.out.printf(" %s ms\n\n", end - start);
        return end - start;
    }

}
