/*
 * The MIT License
 *
 * Copyright (c) 2011, CloudBees, Inc.
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
package hudson.tasks.Ant;
f=namespace(lib.FormTagLib)

if (descriptor.installations.length != 0) {
    f.entry(title:_("Ant Version")) {
        select(class:"setting-input",name:"ant.antName") {
            option(value:"(Default)", _("Default"))
            descriptor.installations.each {
                f.option(selected:it.name==instance?.ant?.name, value:it.name, it.name)
            }
        }
    }
}

f.entry(title:_("Targets"),field:"targets") {
    f.expandableTextbox()
}

def advancedEntries = {
    f.entry(title:_("Build File"),field:"buildFile") {
        f.expandableTextbox()
    }
    f.entry(title:_("Properties"),field:"properties") {
        f.expandableTextbox()
    }
    f.entry(title:_("Java Options"),field:"antOpts") {
        f.expandableTextbox()
    }
}
if(instance.properties || instance.buildFile || instance.antOpts) {
  advancedEntries()
}
else {
  f.advanced(advancedEntries)
}
