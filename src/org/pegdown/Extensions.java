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

public interface Extensions {

    static final int NONE = 0x00;

    static final int SMARTS = 0x01;
    static final int QUOTES = 0x02;
    static final int SMARTYPANTS = 0x03; // see http://daringfireball.net/projects/smartypants/
    static final int ABBREVIATIONS = 0x04;

    static final int ALL = 0xFF;

}
