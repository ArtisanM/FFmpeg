package fm.qingting.audioeditor;

public class AudioUtils {

    public static void adjustVolume(byte[] audioSamples, float volume) {
        for (int i = 0; i < audioSamples.length; i += 2) {
            // convert byte pair to int
            short buf1 = audioSamples[i + 1];
            short buf2 = audioSamples[i];

            buf1 = (short) ((buf1 & 0xff) << 8);
            buf2 = (short) (buf2 & 0xff);

            short res = (short) (buf1 | buf2);
            float v = res * volume;
            if (v > 32767) {
                v = 32767;
            } else if (v < -32768) {
                v = -32768;
            }
            res = (short) v;
            // convert back
            audioSamples[i] = (byte) res;
            audioSamples[i + 1] = (byte) (res >> 8);

        }
    }
}
