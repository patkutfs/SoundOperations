public class PrintingData {
    public static void main(String[] args) {
        VoiceProcess voice = new VoiceProcess();
        try {
            voice.processAudio();

            double[][] numbers = {
                    {voice.dBLeft, voice.dBLeftCenter, voice.dBCenter, voice.dBRightCenter, voice.dBRight},
                    {voice.dBLeft, voice.dBLeftCenter, voice.dBCenter, voice.dBRightCenter, voice.dBRight},
                    {voice.dBLeft, voice.dBLeftCenter, voice.dBCenter, voice.dBRightCenter, voice.dBRight},
                    {voice.dBLeft, voice.dBLeftCenter, voice.dBCenter, voice.dBRightCenter, voice.dBRight}
            };

            for (double[] row : numbers) {
                System.out.printf("--%.2f-----%.2f-----%.2f-----%.2f------%.2f--%n",
                        row[0], row[1], row[2], row[3], row[4]);
            }

            System.out.println("--left-----central-left-----central-----central-right-----right--");
        } catch (Exception e) {
            System.out.println("Error occurred: " + e.getMessage());
        }
    }
}