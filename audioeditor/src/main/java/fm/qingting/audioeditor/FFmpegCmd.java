package fm.qingting.audioeditor;

import android.util.Log;

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
        Log.d("FFmpeg_Editor", printStringArray(cmds));
        sOnCmdExecListener = listener;
        sDuration = duration;

        exec(cmds.length, cmds);
    }

    private static String printStringArray(String[] arr) {
        StringBuilder sb = new StringBuilder();
        for (String s : arr) {
            sb.append(s).append(" ");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
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
