import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.io.File;

public class AudioColumnPanel extends JPanel implements DbUpdateListener {

    private static final int BAR_WIDTH = 50;
    private static final int BAR_SPACING = 20;
    private static final int MAX_BAR_HEIGHT = 200;
    private static final double DB_RANGE_MIN = -60.0;
    private static final double DB_RANGE_MAX = 0.0;

    private static final int NUMBER_OF_COLUMNS = 5;
    private double[] currentDbLevels;

    private VoiceProcess audioProcessor;

    public AudioColumnPanel(String audioFilePath) {
        setBackground(Color.DARK_GRAY);
        setPreferredSize(new Dimension(
                (BAR_WIDTH * NUMBER_OF_COLUMNS) + (BAR_SPACING * (NUMBER_OF_COLUMNS + 1)),
                300));

        currentDbLevels = new double[NUMBER_OF_COLUMNS];
        for (int i = 0; i < NUMBER_OF_COLUMNS; i++) {
            currentDbLevels[i] = DB_RANGE_MIN;
        }

        new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaint();
            }
        }).start();

        try {
            audioProcessor = new VoiceProcess(audioFilePath, this);
            new Thread(audioProcessor).start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Errror loading audio file or sound line: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Could nott load audio file or sound line: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void onDbUpdate(double[] dbLevels) {
        System.arraycopy(dbLevels, 0, this.currentDbLevels, 0, NUMBER_OF_COLUMNS);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        for (int i = 0; i < NUMBER_OF_COLUMNS; i++) {
            double currentDb = currentDbLevels[i];

            double normalizedDb = (currentDb - DB_RANGE_MIN) / (DB_RANGE_MAX - DB_RANGE_MIN);
            int barHeight = (int) (normalizedDb * MAX_BAR_HEIGHT);
            barHeight = Math.max(0, Math.min(barHeight, MAX_BAR_HEIGHT));

            int x = BAR_SPACING + i * (BAR_WIDTH + BAR_SPACING);
            int y = getHeight() - barHeight - 50;

            int depth = 15;
            int offsetX = depth;
            int offsetY = depth;

            g2d.setColor(new Color(150, 200, 255));
            g2d.fillPolygon(new int[]{x + offsetX, x + BAR_WIDTH + offsetX, x + BAR_WIDTH, x},
                    new int[]{y, y, y + depth, y + depth}, 4);

            g2d.setColor(new Color(100, 150, 220));
            g2d.fillPolygon(new int[]{x + BAR_WIDTH + offsetX, x + BAR_WIDTH + offsetX, x + BAR_WIDTH, x + BAR_WIDTH},
                    new int[]{y, y + barHeight, y + barHeight + depth, y + depth}, 4);

            Color barColor;
            if (currentDb > -10) {
                barColor = new Color(255, Math.max(0, Math.min(255, (int)(255 * (1 - normalizedDb)))), 0);
            } else {
                barColor = new Color(0, Math.max(0, Math.min(255, (int)(255 * normalizedDb))), Math.max(0, Math.min(255, (int)(255 * (1 - normalizedDb)))));
            }
            g2d.setColor(barColor);
            g2d.fillRect(x, y + offsetY, BAR_WIDTH, barHeight);

            g2d.setColor(Color.BLACK);
            g2d.drawRect(x, y + offsetY, BAR_WIDTH, barHeight);
            g2d.drawLine(x, y + offsetY, x + offsetX, y);
            g2d.drawLine(x + BAR_WIDTH, y + offsetY, x + BAR_WIDTH + offsetX, y);
            g2d.drawLine(x + offsetX, y, x + BAR_WIDTH + offsetX, y);
            g2d.drawLine(x + BAR_WIDTH, y + offsetY, x + BAR_WIDTH + offsetX, y);
            g2d.drawLine(x + BAR_WIDTH + offsetX, y, x + BAR_WIDTH + offsetX, y + barHeight);
            g2d.drawLine(x + BAR_WIDTH + offsetX, y + barHeight, x + BAR_WIDTH, y + barHeight + offsetY);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            String dbText = String.format("%.1f dB", currentDb);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(dbText);
            g2d.drawString(dbText, x + (BAR_WIDTH - textWidth) / 2, y + offsetY + barHeight + 20);
        }

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        String titleText = "5-Channel Audio Volume Visualizer";
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(titleText);
        g2d.drawString(titleText, (getWidth() - titleWidth) / 2, 30);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("5-Channel Audio Volume Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String audioFilePath = "C:\\Users\\TALHA REİS\\Downloads\\test.wav"; //your path to wav file (only wav)

        File file = new File(audioFilePath);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(frame, "Specifiedd audio file not found:\n" + audioFilePath, "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        AudioColumnPanel panel = new AudioColumnPanel(audioFilePath);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false); //settings
        frame.setVisible(true);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (panel.audioProcessor != null) {
                panel.audioProcessor.stopProcessing();
                System.out.println("Audio processing stopped.");
            }
        }));
    }
}