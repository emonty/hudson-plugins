package hudson.plugins.phing.console;

import hudson.console.LineTransformationOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 *
 * @author Seiji Sogabe
 */
public class PhingConsoleAnnotator extends LineTransformationOutputStream {

    private final OutputStream out;

    private final Charset charset;

    private boolean seenEmptyLine;

    public PhingConsoleAnnotator(OutputStream out, Charset charset) {
        this.out = out;
        this.charset = charset;
    }

    @Override
    protected void eol(byte[] b, int len) throws IOException {

        String line = charset.decode(ByteBuffer.wrap(b, 0, len)).toString();

        // trim off CR/LF from the end
        line = trimEOL(line);

        if (seenEmptyLine && endsWith(line, ':') && line.indexOf('>') > 0) {
            new PhingTargetNote().encodeTo(out);
        }

        if (line.startsWith("Fatal error: ") || line.startsWith("Warning error: ") ||
                line.startsWith("Parse error: ") || line.startsWith("Notice: ")) {
            new PhingPHPErrorNote().encodeTo(out);
        }
        
        if (line.startsWith(("BUILD FINISHED")) || line.startsWith("BUILD FAILED")) {
            new PhingOutcomeNote().encodeTo(out);
        }


        seenEmptyLine = line.length() == 0;
        out.write(b, 0, len);
    }

    private boolean endsWith(String line, char c) {
        int len = line.length();
        return len > 0 && line.charAt(len - 1) == c;
    }

    @Override
    public void close() throws IOException {
        super.close();
        out.close();
    }
}
