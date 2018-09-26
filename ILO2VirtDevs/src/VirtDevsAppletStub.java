import javax.imageio.ImageIO;
import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.applet.AudioClip;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;

public class VirtDevsAppletStub implements AppletStub {
    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public URL getDocumentBase() {
        return Main.baseURL;
    }

    @Override
    public URL getCodeBase() {
        return null;
    }

    @Override
    public String getParameter(String name) {
        return Main.hmap.get(name);
    }

    @Override
    public AppletContext getAppletContext() {
        return new AppletContext() {
            @Override
            public AudioClip getAudioClip(URL url) {
                return null;
            }

            @Override
            public Image getImage(URL url) {
                try {
                    return ImageIO.read(url);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public Applet getApplet(String name) {
                return null;
            }

            @Override
            public Enumeration<Applet> getApplets() {
                return null;
            }

            @Override
            public void showDocument(URL url) {}

            @Override
            public void showDocument(URL url, String target) {}

            @Override
            public void showStatus(String status) {}

            @Override
            public void setStream(String key, InputStream stream) throws IOException {}

            @Override
            public InputStream getStream(String key) {
                return null;
            }

            @Override
            public Iterator<String> getStreamKeys() {
                return null;
            }
        };
    }

    @Override
    public void appletResize(int width, int height) {}
}