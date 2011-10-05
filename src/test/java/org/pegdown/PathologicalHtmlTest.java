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

import org.testng.Assert;
import org.testng.annotations.Test;

public class PathologicalHtmlTest {

    @Test
    public void test() {
        // this test took about 30 seconds to complete in 0.8.5.4
        String content = "" +
                "<table border>\n" +
                "<tr> <th>Your action <th>Partner's action <th>Your jail time <th>Partner's jail time\n" +
                "<tr> <td>silent      <td>silent           <td>1              <td>1\n" +
                "<tr> <td>silent      <td>confess          <td>5              <td>0\n" +
                "<tr> <td>confess     <td>silent           <td>0              <td>5\n" +
                "</table>\n";
        long time = System.currentTimeMillis();
        new PegDownProcessor(Extensions.SMARTYPANTS | Extensions.AUTOLINKS).markdownToHtml(content);
        time = System.currentTimeMillis() - time;
        Assert.assertTrue(time < 1000);
    }

    @Test
    public void test2() {
        // this test took about 30 seconds to complete in 1.0.2
        String content = "" +
                "***a*** ***b*** ***c*** ***d*** ***e*** ***f*** ***g*** ***h*** ***i*** ***f*** " +
                "***g*** ***h*** ***i*** ***j*** ***k*** ***l*** ***m*** ***n*** ***o*** ***p***\n";
        long time = System.currentTimeMillis();
        new PegDownProcessor().markdownToHtml(content);
        time = System.currentTimeMillis() - time;
        Assert.assertTrue(time < 1000);
    }
}
