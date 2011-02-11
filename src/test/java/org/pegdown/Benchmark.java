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
import org.parboiled.Rule;
import org.parboiled.common.FileUtils;
import org.parboiled.common.Preconditions;
import org.parboiled.common.Reference;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.parserunners.ProfilingParseRunner;
import org.pegdown.ast.Node;
import org.pegdown.ast.RootNode;

public class Benchmark {

    @SuppressWarnings({"UnusedAssignment"})
    public static void main(String[] args) {
        System.out.println("pegdown performance test");
        System.out.println("------------------------");

        System.out.print("Creating pegdown processor... :");
        long start = System.currentTimeMillis();
        PegDownProcessor processor = new PegDownProcessor();
        time(start);

        System.out.print("Creating 100 more parser instances... :");
        start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            new PegDownProcessor();
        }
        time(start);

        System.out.print("Parsing benchmark file 100 times... :");
        start = System.currentTimeMillis();
        char[] markdown = FileUtils.readAllCharsFromResource("benchmark.text");
        Preconditions.checkNotNull(markdown, "benchmark file not found");
        for (int i = 0; i < 100; i++) {
            processor.markdownToHtml(markdown);
        }
        time(start);
        
        System.out.println();
        System.out.println("Parsing benchmark once more with ProfileParseRunner...");
        final Reference<ProfilingParseRunner<Node>> profilingRunner = new Reference<ProfilingParseRunner<Node>>();
        Parser parser = Parboiled.createParser(Parser.class, Extensions.NONE, new Parser.ParseRunnerProvider() {
            public ParseRunner<Node> get(Rule rule) {
                if (profilingRunner.isNotSet()) profilingRunner.set(new ProfilingParseRunner<Node>(rule));
                return profilingRunner.get();
            }
        });
        parser.parse(processor.prepareSource(markdown));
        ProfilingParseRunner.Report report = profilingRunner.get().getReport();
        System.out.println();
        System.out.println(report.print());
    }

    private static long time(long start) {
        long end = System.currentTimeMillis();
        System.out.printf(" %s ms\n", end - start);
        return end - start;
    }

}