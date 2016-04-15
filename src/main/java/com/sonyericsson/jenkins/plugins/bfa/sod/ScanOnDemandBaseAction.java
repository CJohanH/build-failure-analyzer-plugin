/*
 * The MIT License
 *
 * Copyright 2012 Sony Mobile Communications AB. All rights reserved.
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
package com.sonyericsson.jenkins.plugins.bfa.sod;

import com.sonyericsson.jenkins.plugins.bfa.Messages;
import com.sonyericsson.jenkins.plugins.bfa.PluginImpl;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseBuildAction;
import com.sonyericsson.jenkins.plugins.bfa.model.FailureCauseMatrixBuildAction;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.Project;
import hudson.model.Result;
import hudson.util.RunList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import org.acegisecurity.AccessDeniedException;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Action class for scanning non scanned build.
 *
 * @author Shemeer Sulaiman &lt;shemeer.x.sulaiman@sonymobile.com&gt;
 */
public class ScanOnDemandBaseAction implements Action {

    /** The project. */
    private Job project;
    /**
     * javascript file location.
     */
    public static final String PLUGIN_JS_URL = "/plugin/build-failure-analyzer/js/";

    /**
     * nonscanned build constant.
     */
    public static final String NON_SCANNED = "nonscanned";

     /** The scan build type. */
    private String buildType;

    /**
     * SODBaseAction constructor.
     *
     * @param project current project.
     */
    public ScanOnDemandBaseAction(final Job project) {
        this.project = project;
    }

    /**
     * Gets the full path to the provided javascript file.
     * For use by jelly files to give to the client browser.
     *
     * @param jsName the javascript filename.
     * @return the full path from the web-context root.
     */
    @SuppressWarnings("unused")
    //called from jelly
    public String getJsUrl(String jsName) {
        return PLUGIN_JS_URL + jsName;
    }

    @Override
    public String getIconFileName() {
        if (hasPermission()) {
            return PluginImpl.getDefaultIcon();
        } else {
            return null;
        }
    }

    @Override
    public String getDisplayName() {
        if (hasPermission()) {
            return Messages.FailureScan_DisplayName();
        } else {
            return null;
        }
    }

    @Override
    public String getUrlName() {
        if (hasPermission()) {
            return "scan-on-demand";
        } else {
            return null;
        }
    }


    /**
     * Checks if the current user has {@link Item#CONFIGURE} or {@link Project#BUILD} permission.
     *
     * @return true if so.
     */
    public boolean hasPermission() {
        return project.hasPermission(Item.CONFIGURE) || project.hasPermission(Project.BUILD);
    }

    /**
     * Checks if the current user has {@link Item#CONFIGURE} or {@link Project#BUILD} permission.
     *
     * @see #hasPermission()
     * @see hudson.security.ACL#checkPermission(hudson.security.Permission)
     */
    public void checkPermission() {
        if (!hasPermission()) {
            throw new AccessDeniedException(
                    Messages.SodAccessDeniedException(Jenkins.getAuthentication().getName(),
                            Item.CONFIGURE.name, Project.BUILD.name));
        }
    }

    /**
     * Returns the project.
     *
     * @return project
     */
    public final Job<?, ?> getProject() {
        return project;
    }

    /**
     * Method for finding all failed builds.
     *
     * @return sodbuilds.
     */
    public List<Run> getAllBuilds() {
        Job currentProject = project;
        List<Run> sodbuilds = new ArrayList<Run>();
        if (currentProject != null) {
            RunList builds = currentProject.getBuilds();
            for (Object build : builds) {
                if (PluginImpl.needToAnalyze(((Run)build).getResult())) {
                    sodbuilds.add((Run)build);
                }
            }
        }
        return sodbuilds;
    }

    /**
     * This method will set the buildType
     * while calling getBuilds from index.jelly.
     *
     * @param scanTarget String.
     * @return builds.
     */
    public List<Run> getBuilds(String scanTarget) {
        if (scanTarget != null) {
            setBuildType(scanTarget);
        }
        return getBuilds();
    }
    /**
     * Method for returning builds as
     * per buildtype.
     *
     * @return builds.
     */
    public List<Run> getBuilds() {
        buildType = getBuildType();
        if (buildType != null) {
            if (buildType.length() == 0 || buildType.equals(NON_SCANNED)) {
                return getNotScannedBuilds();
            } else {
                return getAllBuilds();
            }
        } else {
            return getNotScannedBuilds();
        }
    }

    /**
     * Method for finding sodbuilds.
     *
     * @return sodbuilds.
     */
    public List<Run> getNotScannedBuilds() {
        List<Run> sodbuilds = new ArrayList<Run>();
        if (project != null) {
            List<Run> builds = project.getBuilds();
            for (Run build : builds) {
                final Result result = build.getResult();
                if (result != null
                    && PluginImpl.needToAnalyze(result)
                    && build.getActions(FailureCauseBuildAction.class).isEmpty()
                    && build.getActions(FailureCauseMatrixBuildAction.class).isEmpty()) {

                    sodbuilds.add(build);
                }
            }
        }
        return sodbuilds;
    }

    /**
     * Method for remove matrix run actions.
     *
     * @param build the MatrixBuild.
     */
    public void removeRunActions(MatrixBuild build) {
        List<MatrixRun> runs = build.getRuns();
        for (MatrixRun run : runs) {
            if (run.getNumber() == build.getNumber()) {
                FailureCauseBuildAction fcba = run.getAction(FailureCauseBuildAction.class);
                if (fcba != null) {
                    run.getActions().remove(fcba);
                }
                FailureCauseMatrixBuildAction fcmba = run.getAction(FailureCauseMatrixBuildAction.class);
                if (fcmba != null) {
                    run.getActions().remove(fcmba);
                }
            }
        }
    }

    /**
     * Submit method for running build scan.
     *
     * @param request  StaplerRequest
     * @param response StaplerResponse
     * @throws ServletException if something unfortunate happens.
     * @throws IOException if something unfortunate happens.
     * @throws InterruptedException if something unfortunate happens.
     */
    public void doPerformScan(StaplerRequest request, StaplerResponse response)
            throws ServletException, IOException, InterruptedException {
        checkPermission();
        List<Run> sodbuilds = getBuilds();
        if (!sodbuilds.isEmpty()) {
            for (Run sodbuild : sodbuilds) {
                FailureCauseBuildAction fcba = sodbuild.getAction(FailureCauseBuildAction.class);
                if (fcba != null) {
                    sodbuild.getActions().remove(fcba);
                }
                FailureCauseMatrixBuildAction fcmba = sodbuild.getAction(FailureCauseMatrixBuildAction.class);
                if (sodbuild instanceof MatrixBuild
                        && fcmba != null) {
                    sodbuild.getActions().remove(fcmba);
                    removeRunActions((MatrixBuild)sodbuild);
                }
                ScanOnDemandTask task = new ScanOnDemandTask(sodbuild);
                ScanOnDemandQueue.queue(task);
            }
        }
        response.sendRedirect("../");
    }
    /**
     * Returns the buildType.
     *
     * @return buildType String.
     */
    public String getBuildType() {
        return buildType;
    }

    /**
     * Set buildType.
     *
     * @param buildType String.
     */
    public void setBuildType(String buildType) {
        this.buildType = buildType;
    }
    /**
     * Select buildType.
     *
     * @param scanTarget QueryParameter.
     * @param req StaplerRequest.
     * @param rsp StaplerResponse.
     * @throws ServletException if something unfortunate happens.
     * @throws IOException if something unfortunate happens.
     * @throws InterruptedException if something unfortunate happens.
     */
    public void doSelectBuildType(@QueryParameter("build") String scanTarget,
            StaplerRequest req, StaplerResponse rsp) throws ServletException,
            IOException, InterruptedException {
        if (scanTarget == null) {
            if (req.getSession() != null
                    && req.getSession().getAttribute("buildType") != null) {
                scanTarget = (String)req.getSession().getAttribute("buildType");
            } else {
                scanTarget = NON_SCANNED;
            }
        }
        setBuildType(scanTarget);
        if (req.getSession() != null) {
            req.getSession(true).setAttribute("buildType", buildType);
        }
        rsp.sendRedirect2(".");
    }
}
