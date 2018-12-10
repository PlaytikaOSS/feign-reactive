package reactivefeign.benchmarks;

import java.io.IOException;
import java.io.InputStream;

public class BenchmarkUtils {

    public static String readJsonFromFile(String path) throws IOException {
        InputStream is = BenchmarkUtils.class.getResourceAsStream(path);
        byte data[]=new byte[is.available()];
        is.read(data);
        is.close();
        return new String(data);
    }

    public static byte[] readJsonFromFileAsBytes(String path) throws IOException {
        InputStream is = BenchmarkUtils.class.getResourceAsStream(path);
        byte data[]=new byte[is.available()];
        is.read(data);
        is.close();
        return data;
    }
}
