package org.pegdown;

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
        System.out.println("elapsed time: " + (System.currentTimeMillis() - time) + " milliseconds");
    }
}
