package org.pegdown;

import org.parboiled.ParserStatistics;
import org.parboiled.Rule;

public class ShowParserStats {

    public static void main(String[] args) {
        System.out.print("Creating processor... :");
        long start = System.currentTimeMillis();
        PegDownProcessor processor = new PegDownProcessor();
        time(start);

        Rule rule = processor.getParser().Doc();
        ParserStatistics<Object> stats = ParserStatistics.<Object>generateFor(rule);
        System.out.println(stats);
        System.out.println(stats.printActionClassInstances());
    }

    private static long time(long start) {
        long end = System.currentTimeMillis();
        System.out.printf(" %s ms\n\n", end - start);
        return end - start;
    }

}
