package app.util.compilacao;

/**
 * Helpers puros (sem efeitos colaterais) pra trabalhar com ABIs.
 */
public final class AbiUtils {

    private AbiUtils() { }

    /** Monta o triple do clang pra ABI + API. Ex: aarch64-linux-android21. */
    public static String montarTarget(String abi, int api) {
        if (abi == null) return null;
        if (abi.contains("arm64"))   return "aarch64-linux-android" + api;
        if (abi.contains("armeabi")) return "armv7a-linux-androideabi" + api;
        if (abi.contains("x86_64"))  return "x86_64-linux-android" + api;
        if (abi.contains("x86"))     return "i686-linux-android" + api;
        return null;
    }

    /** Nome da pasta de libs no sysroot. Ex: aarch64-linux-android. */
    public static String montarPastaLib(String abi) {
        if (abi == null) return "aarch64-linux-android";
        if (abi.contains("arm64"))   return "aarch64-linux-android";
        if (abi.contains("armeabi")) return "arm-linux-androideabi";
        if (abi.contains("x86_64"))  return "x86_64-linux-android";
        if (abi.contains("x86"))     return "i686-linux-android";
        return "aarch64-linux-android";
    }

    /** Subpasta do clang-runtime/lib/linux/<aqui>. Ex: aarch64, arm. */
    public static String abiRuntimeDir(String abi) {
        if (abi == null) return "aarch64";
        if (abi.contains("arm64"))   return "aarch64";
        if (abi.contains("armeabi")) return "arm";
        if (abi.contains("x86_64"))  return "x86_64";
        if (abi.contains("x86"))     return "i686";
        return "aarch64";
    }
}