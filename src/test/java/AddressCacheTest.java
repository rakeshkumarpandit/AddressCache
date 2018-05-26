import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AddressCacheTest {

  AddressCache addressCache;
  InetAddress address;

  @Before
  public void init(){
    addressCache = new AddressCache(100, TimeUnit.MILLISECONDS);
  }

  @Test
  public void addAndPeek() throws UnknownHostException {
    address = InetAddress.getByName("127.0.0.1");
    addressCache.add(address);
    Assert.assertEquals(addressCache.peek(), address);
  }

  @Test
  public void getNonExpiredAddress() throws UnknownHostException, InterruptedException {
    address = InetAddress.getByName("127.0.0.1");
    addressCache.add(address);
    TimeUnit.MILLISECONDS.sleep(1000);
    InetAddress address2 = InetAddress.getByName("127.0.0.2");
    addressCache.add(address2);
    InetAddress take = addressCache.take();
    Assert.assertEquals(take, address2);
  }

  @Test
  public void remove() throws UnknownHostException {
    address = InetAddress.getByName("127.0.0.1");
    addressCache.add(address);
    addressCache.remove(address);
    Assert.assertEquals(addressCache.peek(), null);
  }

}
