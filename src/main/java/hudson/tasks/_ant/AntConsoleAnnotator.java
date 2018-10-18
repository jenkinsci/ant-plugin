/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc.
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
package hudson.tasks._ant;

import com.google.common.base.Charsets;
import hudson.console.ConsoleLogFilter;
import hudson.console.LineTransformationOutputStream;
import hudson.model.Run;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import jenkins.util.JenkinsJVM;

/**
 * Filter {@link OutputStream} that places an annotation that marks Ant target execution.
 * 
 * @author Kohsuke Kawaguchi
 * @since 1.349
 */
public class AntConsoleAnnotator extends LineTransformationOutputStream {
    private final OutputStream out;
    private final Charset charset;
    /** Serialized, signed, and Base64-encoded forms of {@link AntTargetNote} and {@link AntOutcomeNote} respectively. */
    private final byte[][] antNotes;

    private boolean seenEmptyLine;

    public AntConsoleAnnotator(OutputStream out, Charset charset) {
        this(out, charset, createAntNotes());
    }

    private AntConsoleAnnotator(OutputStream out, Charset charset, byte[][] antNotes) {
        this.out = out;
        this.charset = charset;
        this.antNotes = antNotes;
    }

    private static byte[][] createAntNotes() {
        JenkinsJVM.checkJenkinsJVM();
        try {
            ByteArrayOutputStream targetNote = new ByteArrayOutputStream();
            new AntTargetNote().encodeTo(targetNote);
            ByteArrayOutputStream outcomeNote = new ByteArrayOutputStream();
            new AntOutcomeNote().encodeTo(outcomeNote);
            return new byte[][] {targetNote.toByteArray(), outcomeNote.toByteArray()};
        } catch (IOException x) { // should be impossible
            throw new RuntimeException(x);
        }
    }

    @Override
    protected void eol(byte[] b, int len) throws IOException {
        String line = charset.decode(ByteBuffer.wrap(b, 0, len)).toString();

        // trim off CR/LF from the end
        line = trimEOL(line);

        if (seenEmptyLine && endsWith(line,':') && line.indexOf(' ')<0)
            // put the annotation
            out.write(antNotes[0]);

        if (line.equals("BUILD SUCCESSFUL") || line.equals("BUILD FAILED"))
            out.write(antNotes[1]);

        seenEmptyLine = line.length()==0;
        out.write(b,0,len);
    }

    private boolean endsWith(String line, char c) {
        int len = line.length();
        return len>0 && line.charAt(len-1)==c;
    }

    @Override
    public void close() throws IOException {
        super.close();
        out.close();
    }

    public static ConsoleLogFilter asConsoleLogFilter() {
        return new ConsoleLogFilterImpl();
    }
    private static class ConsoleLogFilterImpl extends ConsoleLogFilter implements Serializable {
        private static final long serialVersionUID = 1;
        private byte[][] antNotes = createAntNotes();
        private Object readResolve() {
            if (antNotes == null) { // old program.dat
                antNotes = createAntNotes();
            }
            return this;
        }
        @Override public OutputStream decorateLogger(Run build, OutputStream logger) throws IOException, InterruptedException {
            return new AntConsoleAnnotator(logger, Charsets.UTF_8, antNotes);
        }
    }

}
