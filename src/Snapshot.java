import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//imports
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class Snapshot extends  javax.swing.JFrame{
    private JButton pauseButton;
    private JButton captureButton;
    private JButton startButton;
    private JPanel JPanel1;
    private JPanel panelMain;

    // definitions
    final DaemonThread[] myThread = {null};
    int count = 0;
    final VideoCapture[] webSource = {null};

    Mat frame = new Mat();
    MatOfByte mem = new MatOfByte();
    ///
    CascadeClassifier faceDetector = new CascadeClassifier();

    MatOfRect faceDetections = new MatOfRect();
    //////////
    class DaemonThread implements Runnable
    {
        protected volatile boolean runnable = false;

        @Override
        public  void run()
        {
            synchronized(this)
            {
                while(runnable)
                {

                    if(webSource[0].grab())
                    {
                        try
                        {
                            webSource[0].retrieve(frame);
                            Core.flip(frame,frame,1);
                            //FACE
                            //
                            faceDetector.detectMultiScale(frame, faceDetections);
                            for (Rect rect : faceDetections.toArray()) {
                                System.out.println("detected");
                                Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height),
                                        new Scalar(0, 255,0));
                            }

                            Imgcodecs.imencode(".png", frame, mem);
                            Image im = ImageIO.read(new ByteArrayInputStream(mem.toArray()));

                            BufferedImage buff = (BufferedImage) im;
                            Graphics g=JPanel1.getGraphics();


                            if (g.drawImage(buff, 0, 0, buff.getWidth(), buff.getHeight() , 0, 0, buff.getWidth(), buff.getHeight(), null))
                               // System.out.println("W: "+buff.getWidth()+" : H: "+buff.getHeight()); // 640/480
                                if(runnable == false)
                                {
                                    System.out.println("Going to wait()");
                                    this.wait();
                                }
                        }
                        catch(Exception ex)
                        {
                            System.out.println("Error");
                        }
                    }
                }
            }
        }
    }
    //////////
    public static String extract(String jarFilePath){

        if(jarFilePath == null)
            return null;

        // Alright, we don't have the file, let's extract it
        try {
            // Read the file we're looking for
            InputStream fileStream = Snapshot.class.getResourceAsStream(jarFilePath);

            // Was the resource found?
            if(fileStream == null)
                return null;

            // Grab the file name
            String[] chopped = jarFilePath.split("\\/");
            String fileName = chopped[chopped.length-1];

            // Create our temp file (first param is just random bits)
            File tempFile = File.createTempFile("res", fileName);

            // Set this file to be deleted on VM exit
            tempFile.deleteOnExit();

            // Create an output stream to barf to the temp file
            OutputStream out = new FileOutputStream(tempFile);

            // Write the file to the temp file
            byte[] buffer = new byte[1024];
            int len = fileStream.read(buffer);
            while (len != -1) {
                out.write(buffer, 0, len);
                len = fileStream.read(buffer);
            }

            // Store this file in the cache list

            // Close the streams
            fileStream.close();
            out.close();

            // Return the path of this sweet new file
            return tempFile.getAbsolutePath();

        } catch (IOException e) {
            return null;
        }
    }

    public static void main(String args[]) throws IOException, URISyntaxException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println(Core.VERSION);

        JFrame frame = new JFrame("Snapshot");
        frame.setContentPane(new Snapshot().panelMain);
        frame.setSize(655,600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public Snapshot() throws IOException, URISyntaxException {

        frameInit();

        String filePath = Snapshot.extract("resource/haarcascade_frontalface_alt2.xml");

        faceDetector.load(filePath);

        captureButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /// start button
                webSource[0] =new VideoCapture(0);
              //  webSource[0].set(Videoio.CAP_PROP_FRAME_WIDTH, 1280); //1280 640
              //  webSource[0].set(Videoio.CAP_PROP_FRAME_HEIGHT, 800); //800 480
                System.out.println("W: "+webSource[0].get(Videoio.CAP_PROP_FRAME_WIDTH)+" : H: "+webSource[0].get(Videoio.CAP_PROP_FRAME_HEIGHT));
                myThread[0] = new DaemonThread();
                Thread t = new Thread(myThread[0]);
                t.setDaemon(true);
                myThread[0].runnable = true;
                t.start();
                startButton.setEnabled(false);  //start button
                pauseButton.setEnabled(true);  // stop button
            }
        });
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /// stop button
                myThread[0].runnable = false;
                pauseButton.setEnabled(false);
                startButton.setEnabled(true);

                webSource[0].release();
            }
        });
        captureButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ////////////////snapshot button
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter("PNG file","png"));
                int returnVal = (int) chooser.showSaveDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();

                    if (!file.toString().endsWith(".png"))
                        file = new File(file + ".png");
                    Imgcodecs.imwrite(file.getPath(), frame);
                } else {
                    System.out.println("File access cancelled by user.");
                }
            }
        });
    }
}
