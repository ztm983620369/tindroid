package co.tinode.tinodesdk;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lightweight logging facade which works on both Android and plain JVM.
 * <p>
 * On Android it delegates to {@code android.util.Log} via reflection (no compile-time dependency).
 * On JVM it falls back to {@link java.util.logging.Logger}.
 */
public final class TinodeLog {
    private static final Logger JVM_LOGGER = Logger.getLogger("TinodeSDK");

    private static final boolean HAS_ANDROID_LOG;
    private static Method sD;
    private static Method sW;
    private static Method sE;

    static {
        boolean hasAndroid;
        try {
            Class<?> logClass = Class.forName("android.util.Log");
            sD = logClass.getMethod("d", String.class, String.class);
            sW = logClass.getMethod("w", String.class, String.class, Throwable.class);
            sE = logClass.getMethod("e", String.class, String.class, Throwable.class);
            hasAndroid = true;
        } catch (Exception ignored) {
            sD = null;
            sW = null;
            sE = null;
            hasAndroid = false;
        }
        HAS_ANDROID_LOG = hasAndroid;
    }

    private TinodeLog() {
    }

    public static void d(String tag, String message) {
        if (HAS_ANDROID_LOG && sD != null) {
            try {
                sD.invoke(null, tag, message);
                return;
            } catch (Exception ignored) {
            }
        }
        JVM_LOGGER.log(Level.FINE, tag + ": " + message);
    }

    public static void d(String tag, String message, Throwable error) {
        if (error == null) {
            d(tag, message);
            return;
        }
        if (HAS_ANDROID_LOG && sW != null) {
            // Android Log has d(tag,msg,thr) on newer API levels but w(tag,msg,thr) exists broadly.
            try {
                sW.invoke(null, tag, message, error);
                return;
            } catch (Exception ignored) {
            }
        }
        JVM_LOGGER.log(Level.FINE, tag + ": " + message, error);
    }

    public static void w(String tag, String message) {
        w(tag, message, null);
    }

    public static void w(String tag, String message, Throwable error) {
        if (HAS_ANDROID_LOG && sW != null) {
            try {
                sW.invoke(null, tag, message, error);
                return;
            } catch (Exception ignored) {
            }
        }
        if (error == null) {
            JVM_LOGGER.log(Level.WARNING, tag + ": " + message);
        } else {
            JVM_LOGGER.log(Level.WARNING, tag + ": " + message, error);
        }
    }

    public static void e(String tag, String message, Throwable error) {
        if (HAS_ANDROID_LOG && sE != null) {
            try {
                sE.invoke(null, tag, message, error);
                return;
            } catch (Exception ignored) {
            }
        }
        JVM_LOGGER.log(Level.SEVERE, tag + ": " + message, error);
    }
}
