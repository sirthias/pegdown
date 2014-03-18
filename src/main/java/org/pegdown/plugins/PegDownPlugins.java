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

package org.pegdown.plugins;

import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;

import java.util.*;

/**
 * Encapsulates the plugins provided to pegdown.
 *
 * Construct this using @{link PegdownPlugins#builder}, and then passing in either the Java plugin classes, or
 * precompiled rules (for greater control, or if using Scala rules).
 */
public class PegDownPlugins {

    private final Rule[] inlinePluginRules;
    private final Rule[] blockPluginRules;
    private final Character[] specialChars;
    private final List<ToHtmlSerializerPlugin> serializerPlugins;

    private PegDownPlugins(Rule[] inlinePluginRules, Rule[] blockPluginRules) {
        this(inlinePluginRules, blockPluginRules, new Character[0], Collections.<ToHtmlSerializerPlugin>emptyList());
    }

    private PegDownPlugins(Rule[] inlinePluginRules, Rule[] blockPluginRules, Character[] specialChars, List<ToHtmlSerializerPlugin> serializerPlugins) {
        this.inlinePluginRules = inlinePluginRules;
        this.blockPluginRules = blockPluginRules;
        this.specialChars = specialChars;
        this.serializerPlugins = serializerPlugins;
    }

    public Rule[] getInlinePluginRules() {
        return inlinePluginRules;
    }

    public Rule[] getBlockPluginRules() {
        return blockPluginRules;
    }

    public Character[] getSpecialChars() {
        return specialChars;
    }

    public List<ToHtmlSerializerPlugin> getHtmlSerializerPlugins() {
    	return serializerPlugins;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a builder that is a copy of the existing plugins
     */
    public static Builder builder(PegDownPlugins like) {
        return builder().withInlinePluginRules(like.getInlinePluginRules()).withBlockPluginRules(like.getBlockPluginRules()).
        		withHtmlSerializer(like.serializerPlugins.toArray(new ToHtmlSerializerPlugin[0]));
    }

    /**
     * Convenience reference to no plugins.
     */
    public static PegDownPlugins NONE = builder().build();

    public static class Builder {
        private final List<Rule> inlinePluginRules = new ArrayList<Rule>();
        private final List<Rule> blockPluginRules = new ArrayList<Rule>();
        private final Set<Character> specialChars = new HashSet<Character>();
        private final List<ToHtmlSerializerPlugin> serializerPlugins = new ArrayList<ToHtmlSerializerPlugin>();

        public Builder() {
        }

        public Builder withInlinePluginRules(Rule... inlinePlugins) {
            this.inlinePluginRules.addAll(Arrays.asList(inlinePlugins));
            return this;
        }

        public Builder withBlockPluginRules(Rule... blockPlugins) {
            this.blockPluginRules.addAll(Arrays.asList(blockPlugins));
            return this;
        }

        public Builder withSpecialChars(Character... chars) {
            Collections.addAll(this.specialChars, chars);
            return this;
        }
        
        public Builder withHtmlSerializer(ToHtmlSerializerPlugin... plugins) {
        	Collections.addAll(this.serializerPlugins, plugins);
        	return this;
        }

        /**
         * Add a plugin parser.  This should either implement {@link InlinePluginParser} or {@link BlockPluginParser},
         * or both.  The parser will be enhanced by parboiled before its rules are extracted and registered here.
         *
         * @param pluginParser the plugin parser class.
         * @param arguments the arguments to pass to the constructor of that class.
         */
        public Builder withPlugin(Class<? extends BaseParser<Object>> pluginParser, Object... arguments) {
            // First, check that the parser implements one of the parser interfaces
            if (!(InlinePluginParser.class.isAssignableFrom(pluginParser) ||
                    BlockPluginParser.class.isAssignableFrom(pluginParser))) {
                throw new IllegalArgumentException("Parser plugin must implement a parser plugin interface to be useful");
            }
            BaseParser<Object> parser = Parboiled.createParser(pluginParser, arguments);
            if (parser instanceof InlinePluginParser) {
                withInlinePluginRules(((InlinePluginParser) parser).inlinePluginRules());
            }
            if (parser instanceof BlockPluginParser) {
                withBlockPluginRules(((BlockPluginParser) parser).blockPluginRules());
            }
            return this;
        }
        
        public PegDownPlugins build() {
            return new PegDownPlugins(inlinePluginRules.toArray(new Rule[0]), blockPluginRules.toArray(new Rule[0]),
                    specialChars.toArray(new Character[0]), Collections.unmodifiableList(serializerPlugins));
        }
    }
}
