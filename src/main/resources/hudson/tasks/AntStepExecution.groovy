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

package hudson.tasks

import hudson.tasks._ant.AntConsoleAnnotator
import org.jenkinsci.plugins.workflow.cps.steps.ingroovy.GroovyStepExecution

public class AntStepExecution extends GroovyStepExecution {

    def call() {
        def unix = isUnix()
        def exe = step.tool != null ? (unix ? "${tool step.tool}/bin/ant" : "${tool step.tool}\\bin\\ant.bat") : 'ant'
        withContext(AntConsoleAnnotator.asConsoleLogFilter()) {
            // TODO some complicated logic in Ant.toWindowsCommand with no apparent test coverage; keeping it simple for now
            // if necessary could factor heavy logic into AntStep: String createCommandLine(@CheckForNull String toolHome, boolean unix)
            def run = {unix ? sh("'${exe}'${step.targets ? ' ' + step.targets : ''}") : bat("\"${exe}\"${step.targets ? ' ' + step.targets : ''}")}
            step.opts ? withEnv(["ANT_OPTS=${step.opts}"]) {run()} : run()
        }
    }

}
