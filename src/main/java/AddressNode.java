import java.net.InetAddress;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AddressNode {

  private InetAddress address;
  private LocalDateTime enqueueTime;
}
