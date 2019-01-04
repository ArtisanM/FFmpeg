package fm.qingting.audioeditor;

public class FFmpegCmd {

    static {
        System.loadLibrary("ffmpeg-cmd");
    }

    private static OnCmdExecListener sOnCmdExecListener;
    private static long sDuration; // ffmpeg需要处理的media outputFile总时长 ms

    private static native int exec(int argc, String[] argv);

    public static native void exit();

    /**
     * @param cmds     ffmpeg command
     * @param duration ms
     * @param listener callback
     */
    public static void exec(String[] cmds, long duration, OnCmdExecListener listener) {
        sOnCmdExecListener = listener;
        sDuration = duration;

        exec(cmds.length, cmds);
    }

    /**
     * FFmpeg执行结束回调，由C代码中调用
     */
    public static void onExecuted(int ret) {
        if (sOnCmdExecListener != null) {
            if (ret == 0) {
                sOnCmdExecListener.onProgress(1);
                sOnCmdExecListener.onSuccess();
            } else {
                sOnCmdExecListener.onFailure();
            }
        }
        sDuration = 0;
    }

    /**
     * FFmpeg执行进度回调，由C代码调用
     */
    public static void onProgress(float progress) {
        if (sOnCmdExecListener != null) {
            if (sDuration != 0) {
                sOnCmdExecListener.onProgress(progress / (sDuration / 1000f));
            }
        }
    }

    public interface OnCmdExecListener {

        void onSuccess();

        void onFailure();

        void onProgress(float progress);
    }
}
