/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
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
package hudson.tasks.AntWrapper;
f=namespace(lib.FormTagLib)

def installations = app.getDescriptorByType(hudson.tasks.Ant.DescriptorImpl).installations
if (installations.length != 0) {
    f.entry(title: _('Ant Version')) {
        select(class: 'setting-input', name: 'ant.installation') {
            option(value: '', _('Default'))
            installations.each {
                f.option(selected: it.name == instance?.installation, value: it.name, it.name)
            }
        }
    }
}

def jdks = app.getDescriptorByType(hudson.model.JDK.DescriptorImpl).installations
if (jdks.length != 0) {
    f.entry(title: _('JDK')) {
        select(class: 'setting-input', name: 'ant.jdk') {
            option(value: '', _('Default'))
            jdks.each {
                f.option(selected: it.name == instance?.jdk, value: it.name, it.name)
            }
        }
    }
}
