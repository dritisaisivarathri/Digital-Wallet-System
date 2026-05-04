import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class CheckBcrypt {
  public static void main(String[] args) {
    System.out.println(new BCryptPasswordEncoder().matches(args[0], args[1]));
  }
}
