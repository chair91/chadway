package org.adaway.util;

import android.content.Context;

import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.adaway.util.Constants.TAG;

/**
 * This class is an utility class to help with shell commands.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public final class ShellUtils {
    private static final String EXECUTABLE_PREFIX = "lib";
    private static final String EXECUTABLE_SUFFIX = "_exec.so";

    /**
     * Private constructor.
     */
    private ShellUtils() {

    }

    public static String getFirstLine(List<String> lines) {
        return lines.isEmpty() ? "" : lines.get(0);
    }

    public static String mergeAllLines(List<String> lines) {
        return String.join("\n", lines);
    }

    public static boolean isBundledExecutableRunning(String executable) {
        return Shell.su("ps -a | grep " + EXECUTABLE_PREFIX + executable + EXECUTABLE_SUFFIX).exec().isSuccess();
    }

    public static boolean runBundledExecutable(Context context, String executable, String parameters) {
        String nativeLibraryDir = context.getApplicationInfo().nativeLibraryDir;
        String command = nativeLibraryDir + File.separator + EXECUTABLE_PREFIX + executable + EXECUTABLE_SUFFIX + " " + parameters + " &";
        return Shell.su(command).exec().isSuccess();
    }

    public static void killBundledExecutable(String executable) {
        Shell.su("killall " + EXECUTABLE_PREFIX + executable + EXECUTABLE_SUFFIX).exec();
    }

    public static boolean remountPartition(File file, MountType type) {
        Optional<String> partitionOptional = findPartition(file);
        if (!partitionOptional.isPresent()) {
            return false;
        }
        String partition = partitionOptional.get();
        Shell.Result result = Shell.su("mount -o " + type.getOption() + ",remount " + partition).exec();
        boolean success = result.isSuccess();
        if (!success) {
            Log.w(TAG, "Failed to remount partition " + partition + " as " + type.getOption() + ": " + mergeAllLines(result.getErr()));
        }
        return success;
    }

    private static Optional<String> findPartition(File file) {
        // Get mount points
        Shell.Result result = Shell.su("cat /proc/mounts | cut -d ' ' -f2").exec();
        List<String> out = result.getOut();
        // Check file and each parent against mount points
        while (file != null) {
            String path = file.getAbsolutePath();
            for (String mount : out) {
                if (path.equals(mount)) {
                    return Optional.of(mount);
                }
            }
            // TODO https://github.com/topjohnwu/libsu/pull/35
//            file = file.getParentFile();
            file = file.getParent() == null ? null : file.getParentFile();
        }
        return Optional.empty();
    }
}
