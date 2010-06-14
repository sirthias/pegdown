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

import org.parboiled.google.base.Preconditions;

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
            processor = new PegDownProcessor();
        }
        time(start);

        System.out.print("Parsing benchmark file 100 times... :");
        start = System.currentTimeMillis();
        String markdown = FileUtils.readAllTextFromResource("benchmark.text");
        Preconditions.checkNotNull(markdown, "benchmark file not found");
        for (int i = 0; i < 100; i++) {
            processor.markdownToHtml(markdown);
        }
        time(start);
    }

    private static long time(long start) {
        long end = System.currentTimeMillis();
        System.out.printf(" %s ms\n", end - start);
        return end - start;
    }

}