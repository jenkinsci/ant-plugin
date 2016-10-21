/*
 * The MIT License
 *
 * Copyright 2016 CloudBees, Inc.
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

package hudson.tasks;

import hudson.Extension;
import hudson.Util;
import javax.annotation.CheckForNull;
import org.jenkinsci.plugins.workflow.cps.steps.ingroovy.GroovyStep;
import org.jenkinsci.plugins.workflow.cps.steps.ingroovy.GroovyStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class AntStep extends GroovyStep {

    private final String targets;
    private String tool;
    private String opts;

    @DataBoundConstructor public AntStep(String targets) {
        this.targets = fixEmptyMultiline(targets);
    }

    public String getTargets() {
        return targets;
    }

    public String getTool() {
        return tool;
    }

    @DataBoundSetter public void setTool(String tool) {
        this.tool = Util.fixEmpty(tool);
    }

    public String getOpts() {
        return opts;
    }

    @DataBoundSetter public void setOpts(String opts) {
        this.opts = fixEmptyMultiline(opts);
    }

    private static @CheckForNull String fixEmptyMultiline(@CheckForNull String s) {
        s = Util.fixEmpty(s);
        return s != null ? s.replaceAll("\\s+", " ").trim() : null;
    }

    @Extension public static class DescriptorImpl extends GroovyStepDescriptor {

        @Override public String getFunctionName() {
            return "ant";
        }

        @Override public String getDisplayName() {
            return hudson.tasks._ant.Messages.Ant_DisplayName();
        }

    }

}
