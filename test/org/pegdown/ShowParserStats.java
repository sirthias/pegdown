package org.pegdown;

import org.parboiled.ParserStatistics;
import org.parboiled.Rule;

public class ShowParserStats {

    public static void main(String[] args) {
        Rule rule = new PegDownProcessor().getParser().Doc();
        ParserStatistics<Object> stats = ParserStatistics.<Object>generateFor(rule);
        System.out.println(stats);
        System.out.println(stats.printActionClassInstances());
    }

}
