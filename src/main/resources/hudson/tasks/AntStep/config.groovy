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
package hudson.tasks.AntStep;
f=namespace(lib.FormTagLib)

def installations = app.getDescriptorByType(hudson.tasks.Ant.DescriptorImpl).installations

if (installations.length != 0) {
    f.entry(title:_("Ant Version")) {
        select(class:"setting-input",name:"ant.tool") {
            option(value:"", _("Default"))
            installations.each {
                f.option(selected:it.name==instance?.tool, value:it.name, it.name)
            }
        }
    }
}

f.entry(title:_("Targets"),field:"targets") {
    f.expandableTextbox()
}

f.advanced {
    f.entry(title:_("Java Options"),field:"opts") {
        f.expandableTextbox()
    }
}
