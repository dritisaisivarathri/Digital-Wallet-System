import java.io.*;
import java.util.jar.*;
public class DumpAuthService {
  public static void main(String[] args) throws Exception {
    try (JarFile jar = new JarFile(args[0])) {
      JarEntry entry = jar.getJarEntry("BOOT-INF/classes/com/wallet/auth/service/AuthService.class");
      try (InputStream in = jar.getInputStream(entry)) {
        byte[] bytes = in.readAllBytes();
        String s = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        System.out.println(s.contains("@admin.com"));
        System.out.println(s.contains("findUserForLogin"));
        System.out.println(s.contains("refreshToken"));
      }
    }
  }
}
