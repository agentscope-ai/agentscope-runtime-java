package runtime.domain.tools.service;

public final class TestUtil {

    private TestUtil() {}

    public static boolean shouldRunIntegration() {
        String flag = System.getenv("RUN_SANDBOX_INTEGRATION");
        return flag != null && (flag.equalsIgnoreCase("1") || flag.equalsIgnoreCase("true"));
    }
}


