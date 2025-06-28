import javax.sound.sampled.*;
import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class VoiceProcess implements Runnable {
    private AudioInputStream audioInputStream;
    private AudioFormat format;
    private DbUpdateListener listener;
    private SourceDataLine audioLine;

    private volatile boolean running = true;

    private double[] currentDbLevels = new double[5];

    public VoiceProcess(String filePath, DbUpdateListener listener) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        this.audioInputStream = AudioSystem.getAudioInputStream(new File(filePath));
        this.format = audioInputStream.getFormat();
        this.listener = listener;

        if (format.getChannels() != 2) {
            throw new IllegalArgumentException("Audio must be stereo.");
        }
        if (format.getSampleSizeInBits() != 16) {
            throw new IllegalArgumentException("Only 16-bit audio is supported.");
        }

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        audioLine = (SourceDataLine) AudioSystem.getLine(info);
        audioLine.open(format);
        audioLine.start();
    }

    @Override
    public void run() {
        double windowTimeSeconds = 0.05;
        int frameSize = format.getFrameSize();
        int bytesPerRead = (int) (format.getFrameRate() * windowTimeSeconds * frameSize);

        if (bytesPerRead % frameSize != 0) {
            bytesPerRead = (bytesPerRead / frameSize) * frameSize;
        }
        if (bytesPerRead == 0) {
            bytesPerRead = frameSize * 10;
        }

        byte[] buffer = new byte[bytesPerRead];
        boolean isBigEndian = format.isBigEndian();

        try {
            while (running) {
                int bytesRead = audioInputStream.read(buffer, 0, buffer.length);
                if (bytesRead == -1) {
                    System.out.println("Audio file processing completed.");
                    running = false;
                    double[] zeroDbs = new double[5];
                    for(int i = 0; i < 5; i++) zeroDbs[i] = -60.0;
                    SwingUtilities.invokeLater(() -> listener.onDbUpdate(zeroDbs));
                    break;
                }

                audioLine.write(buffer, 0, bytesRead);

                int numFrames = bytesRead / frameSize;
                if (numFrames == 0) continue;

                double sumLeft = 0.0, sumLeftCenter = 0.0, sumCenter = 0.0;
                double sumRightCenter = 0.0, sumRight = 0.0;

                for (int i = 0; i < bytesRead; i += frameSize) {
                    short leftSample, rightSample;
                    if (isBigEndian) {
                        leftSample = (short) ((buffer[i] << 8) | (buffer[i + 1] & 0xFF));
                        rightSample = (short) ((buffer[i + 2] << 8) | (buffer[i + 3] & 0xFF));
                    } else {
                        leftSample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                        rightSample = (short) ((buffer[i + 3] << 8) | (buffer[i + 2] & 0xFF));
                    }

                    sumLeft += leftSample * leftSample;
                    sumLeftCenter += (0.75 * leftSample * leftSample) + (0.25 * rightSample * rightSample);
                    sumCenter += (0.5 * leftSample * leftSample) + (0.5 * rightSample * rightSample);
                    sumRightCenter += (0.25 * leftSample * leftSample) + (0.75 * rightSample * rightSample);
                    sumRight += rightSample * rightSample;
                }

                final double rmsLeft = Math.sqrt(sumLeft / numFrames);
                final double rmsLeftCenter = Math.sqrt(sumLeftCenter / numFrames);
                final double rmsCenter = Math.sqrt(sumCenter / numFrames);
                final double rmsRightCenter = Math.sqrt(sumRightCenter / numFrames);
                final double rmsRight = Math.sqrt(sumRight / numFrames);

                currentDbLevels[0] = convertRmsToDb(rmsLeft);
                currentDbLevels[1] = convertRmsToDb(rmsLeftCenter);
                currentDbLevels[2] = convertRmsToDb(rmsCenter);
                currentDbLevels[3] = convertRmsToDb(rmsRightCenter);
                currentDbLevels[4] = convertRmsToDb(rmsRight);

                SwingUtilities.invokeLater(() -> listener.onDbUpdate(currentDbLevels.clone()));
            }
        } catch (IOException e) {
            System.err.println("Error reading audio file: " + e.getMessage());
        } finally {
            try {
                if (audioInputStream != null) {
                    audioInputStream.close();
                }
                if (audioLine != null) {
                    audioLine.drain();
                    audioLine.stop();
                    audioLine.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing audio stream: " + e.getMessage());
            }
        }
    }

    private double convertRmsToDb(double rms) {
        if (rms == 0) {
            return -90.0;
        }
        return 20 * Math.log10(rms / 32767.0);
    }

    public void stopProcessing() {
        running = false;
        if (audioLine != null) {
            audioLine.stop();
            audioLine.close();
        }
    }
}