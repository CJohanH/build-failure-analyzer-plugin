/*
 * The MIT License
 *
 * Copyright 2012 Sony Ericsson Mobile Communications. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.sonyericsson.jenkins.plugins.bfa;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCause;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Shell;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Tests for the config page being transient.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class TransientCauseManagementHudsonTest extends HudsonTestCase {

    /**
     * Tests that the {@link CauseManagement} link is present on a freestyle project and that you can navigate to it.
     *
     * @throws Exception if so.
     */
    public void testOnAProject() throws Exception {
        FreeStyleProject project = createFreeStyleProject("nils-job");
        project.getBuildersList().add(new Shell("env | sort"));
        project = configRoundtrip(project);
        WebClient web = createWebClient();
        HtmlPage page = web.goTo("/" + project.getUrl());
        try {
            HtmlAnchor anchor = getAnchorBySuffix(page, CauseManagement.URL_NAME);
            HtmlPage configPage = anchor.click();
            verifyIsConfigurationPage(configPage);
        } catch (ElementNotFoundException e) {
            fail("The link should be there! " + e.getMessage());
        }
    }

    /**
     * Tests that the {@link CauseManagement} link is present on a freestyle build and that you can navigate to it.
     *
     * @throws Exception if so.
     */
    public void testOnABuild() throws Exception {
        FreeStyleProject project = createFreeStyleProject("nils-job");
        FreeStyleBuild build = buildAndAssertSuccess(project);
        //TODO let the system provide it when that is implemented.
        build.addAction(new FailureCauseBuildAction(Collections.<FailureCause>emptyList()));
        WebClient web = createWebClient();
        HtmlPage page = web.goTo("/" + build.getUrl());
        try {
            HtmlAnchor anchor = getAnchorBySuffix(page, "../" + CauseManagement.URL_NAME);
            HtmlPage configPage = anchor.click();
            verifyIsConfigurationPage(configPage);
        } catch (ElementNotFoundException e) {
            fail("The link should be there! " + e.getMessage());
        }
    }

    //CS IGNORE JavadocMethod FOR NEXT 10 LINES. REASON: The exception can be thrown.

    /**
     * Finds an anchor on the page who's href attribute ends with the provided suffix.
     *
     * @param page   the page
     * @param suffix the suffix
     * @return the anchor
     *
     * @throws ElementNotFoundException if no anchor was found.
     */
    private HtmlAnchor getAnchorBySuffix(HtmlPage page, String suffix) {
        List<HtmlAnchor> anchors = page.getAnchors();
        for (HtmlAnchor anchor : anchors) {
            if (anchor.getHrefAttribute().endsWith(suffix)) {
                return anchor;
            }
        }
        throw new ElementNotFoundException("a", "href", ".*" + suffix);
    }

    /**
     * Verifies that the provided page is the cause management page, by doing some checks on specific elements.
     *
     * @param page the page to verify.
     * @throws IOException if so.
     */
    private void verifyIsConfigurationPage(HtmlPage page) throws IOException {
        try {
            //Some smoke test to see if it is the correct page
            HtmlForm form = page.getFormByName("causesForm");
            form.getElementsByAttribute("button", "name", "btnSubmit");
            DomNodeList<HtmlElement> elementsByTagName = page.getElementsByTagName("h1");
            boolean headingFound = false;
            for (HtmlElement element : elementsByTagName) {
                if ("Update Failure Causes".equals(element.getTextContent())) {
                    headingFound = true;
                    break;
                }
            }
            assertTrue("H1 Heading not found!", headingFound);
        } catch (ElementNotFoundException e) {
            fail("The element should be there! " + e.getMessage());
        }
    }
}
