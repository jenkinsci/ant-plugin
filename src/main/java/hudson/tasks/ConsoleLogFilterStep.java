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

import com.google.inject.Inject;
import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.BodyInvoker;
import org.kohsuke.stapler.DataBoundConstructor;

// TODO could perhaps be moved somewhere generic?

public class ConsoleLogFilterStep extends AbstractStepImpl {

    public final ConsoleLogFilter delegate;
    
    @DataBoundConstructor public ConsoleLogFilterStep(ConsoleLogFilter delegate) {
        this.delegate = delegate;
    }

    public static class Execution extends AbstractStepExecutionImpl {
        
        private static final long serialVersionUID = 1;

        @Inject(optional=true) private transient ConsoleLogFilterStep step;
        
        @Override public boolean start() throws Exception {
            getContext().newBodyInvoker().withContext(BodyInvoker.mergeConsoleLogFilters(getContext().get(ConsoleLogFilter.class), step.delegate)).withCallback(BodyExecutionCallback.wrap(getContext())).start();
            return false;
        }
        
        @Override public void stop(Throwable cause) throws Exception {
            getContext().onFailure(cause);
        }

    }
    
    @Extension public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override public String getFunctionName() {
            return "consoleLogFilter";
        }

        @Override public boolean isAdvanced() {
            return true;
        }

        @Override public boolean takesImplicitBlockArgument() {
            return true;
        }

    }

}
