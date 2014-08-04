package com.video;

import com.model.Archive;
import com.model.Settings;
import com.server.Server;

import java.io.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by calc on 26.07.14.
 *
 */
public class FFmpeg {
    private static Logger log = Logger.getLogger("main");

    public void deleteOld(String archive){
        File f = new File(Settings.getInstance().getFullTmpPath() + archive);
        f.delete();
    }

    /**
     * https://trac.ffmpeg.org/wiki/How%20to%20concatenate%20(join,%20merge)%20media%20files
     * @param list of Archives
     * @throws IOException
     */
    public void createConcatFile(List<Archive> list) throws IOException {
        /*
        # this is a comment
        file '/path/to/file1'
        file '/path/to/file2'
        file '/path/to/file3'
         */

        File concat = new File("concat.txt");

        if(concat.exists())
            concat.delete();

        concat.createNewFile();

        FileWriter writer = new FileWriter(concat);
        for(Archive archive : list){
            String path = Settings.getInstance().getFullRecPath() + archive.getFileName() + ".mp4";
            File file = new File(path);
            if(!file.exists()){
                log.warning(String.format("file %s not exist", path));
                continue;
            }
            String line = String.format("file '%s'\r\n", path);
            writer.write(line);
        }

        writer.flush();
        writer.close();
    }

    public void concat(){
        String command = String.format("%s -y -f concat -i concat.txt -codec copy concat.mp4",
                Settings.getInstance().getFfmpegPath());
        log.info(command);

        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(command);

            /*InputStream stdin = pr.getInputStream();
            InputStreamReader isr = new InputStreamReader(stdin);
            BufferedReader br = new BufferedReader(isr);

            String line = null;
            System.out.println("<OUTPUT>");

            while ( (line = br.readLine()) != null)
                System.out.println(line);

            System.out.println("</OUTPUT>");*/

            int exitVal = pr.waitFor();
            if(exitVal != 0)
                log.warning("FFmpeg " + command + " exited with error code " + exitVal);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void move(String archive){
        try {
            Settings settings = Settings.getInstance();
            String command = String.format("%s -i %s -codec copy %s",
                    settings.getFfmpegPath(),
                    settings.getFullTmpPath() + archive,
                    settings.getArchivePath() + "/" + settings.getRecPath() + "/" + archive + ".mp4");

            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(command);

            int exitVal = pr.waitFor();
            if(exitVal == 0)
                deleteOld(archive);
            else
                log.warning("FFmpeg " + command + " exited with error code " + exitVal);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
