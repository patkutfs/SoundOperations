import javax.sound.sampled.*;
import java.io.File;

public class VoiceProcess {
    private AudioInputStream audioInputStream;
    private AudioFormat format;

    public double dBLeft;
    public double dBLeftCenter;
    public double dBCenter;
    public double dBRightCenter;
    public double dBRight;

    public void processAudio() throws Exception {
        String filePath = "your path to wav file";
        try {
            audioInputStream = AudioSystem.getAudioInputStream(new File(filePath));
            format = audioInputStream.getFormat();
            checkFormat();
            processAudioData();
        } finally {
            closeStream();
        }
    }

    public void checkFormat() throws Exception {
        if (format.getChannels() != 2) {
            throw new Exception("Audio is not stereo");
        }
        if (format.getSampleSizeInBits() != 16) {
            throw new Exception("Only 16-bit audio is supported");
        }
    }

    public void processAudioData() throws Exception {
        double windowTime = 0.1;
        int frameSize = format.getFrameSize();
        int chunkSize = (int) (format.getFrameRate() * windowTime * frameSize);
        byte[] buffer = new byte[chunkSize];
        boolean isBigEndian = format.isBigEndian();

        double sumLeft = 0.0, sumLeftCenter = 0.0, sumCenter = 0.0;
        double sumRightCenter = 0.0, sumRight = 0.0;
        int totalFrames = 0;

        while (true) {
            int bytesRead = audioInputStream.read(buffer, 0, chunkSize);
            if (bytesRead == -1) break;
            int numFrames = bytesRead / frameSize;
            totalFrames += numFrames;

            for (int i = 0; i < bytesRead; i += frameSize) {
                short left, right;
                if (isBigEndian) {
                    left = (short) ((buffer[i] << 8) | (buffer[i + 1] & 0xFF));
                    right = (short) ((buffer[i + 2] << 8) | (buffer[i + 3] & 0xFF));
                } else {
                    left = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                    right = (short) ((buffer[i + 3] << 8) | (buffer[i + 2] & 0xFF));
                }

                sumLeft += left * left;
                sumLeftCenter += (0.75 * left * left) + (0.25 * right * right);
                sumCenter += (0.5 * left * left) + (0.5 * right * right);
                sumRightCenter += (0.25 * left * left) + (0.75 * right * right);
                sumRight += right * right;
            }
        }

        if (totalFrames > 0) {
            double rmsLeft = Math.sqrt(sumLeft / totalFrames);
            double rmsLeftCenter = Math.sqrt(sumLeftCenter / totalFrames);
            double rmsCenter = Math.sqrt(sumCenter / totalFrames);
            double rmsRightCenter = Math.sqrt(sumRightCenter / totalFrames);
            double rmsRight = Math.sqrt(sumRight / totalFrames);

            dBLeft = Double.isNaN(20 * Math.log10(rmsLeft / 32767.0)) ? -90.0 : 20 * Math.log10(rmsLeft / 32767.0);
            dBLeftCenter = Double.isNaN(20 * Math.log10(rmsLeftCenter / 32767.0)) ? -90.0 : 20 * Math.log10(rmsLeftCenter / 32767.0);
            dBCenter = Double.isNaN(20 * Math.log10(rmsCenter / 32767.0)) ? -90.0 : 20 * Math.log10(rmsCenter / 32767.0);
            dBRightCenter = Double.isNaN(20 * Math.log10(rmsRightCenter / 32767.0)) ? -90.0 : 20 * Math.log10(rmsRightCenter / 32767.0);
            dBRight = Double.isNaN(20 * Math.log10(rmsRight / 32767.0)) ? -90.0 : 20 * Math.log10(rmsRight / 32767.0);
        }
    }

    public void closeStream() throws Exception {
        if (audioInputStream != null) {
            audioInputStream.close();
        }
    }

    public static void main(String[] args) {
        VoiceProcess voice = new VoiceProcess();
        try {
            voice.processAudio();
            System.out.println("Left Channel dB: " + voice.dBLeft);
            System.out.println("Left-Center dB: " + voice.dBLeftCenter);
            System.out.println("Center dB: " + voice.dBCenter);
            System.out.println("Right-Center dB: " + voice.dBRightCenter);
            System.out.println("Right Channel dB: " + voice.dBRight);
        } catch (Exception e) {
            System.out.println("Error occurred: " + e.getMessage());
        }
    }
}
