import com.video.Recorder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by calc on 14.07.14.
 *
 */
public class Main {

    private static Thread t;
    public static void main(String[] args) {

        final Recorder recorder = new Recorder();
        final URL url;
        try {
            //url = new URL("http://10.154.28.202:9012/path.mp4");
            url = new URL("http://10.154.28.203:9019/path.mp4");

            t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        recorder.open(url);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            t.start();

            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            recorder.close();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

}
