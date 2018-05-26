import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;

/*
 * The AddressCache has a max age for the elements it's storing, an add method 
 * for adding elements, a remove method for removing, a peek method which 
 * returns the most recently added element, and a take method which removes 
 * and returns the most recently added element.
 */

@Log4j2
public class AddressCache {

  private BlockingQueue<InetAddress> addressQueue;
  private ConcurrentHashMap<InetAddress, List<AddressNode>> addressMap;
  private long evictionTimeInMillis;
  private int MAX_SIZE;

	public AddressCache(long maxAge, TimeUnit unit) {
	  if(Objects.isNull(unit)){
	    log.error("TimeUnit not specified while initialization");
	    throw new IllegalArgumentException("TimeUnit missing");
    }
    if(maxAge <= 0){
      log.error("maxAge should be greater than zero");
      throw new IllegalArgumentException("maxAge is invalid");
    }
    MAX_SIZE = 10000;
	  addressQueue = new ArrayBlockingQueue<>(MAX_SIZE);
	  evictionTimeInMillis = unit.toMillis(maxAge);
	  addressMap = new ConcurrentHashMap<>();
	}
	
	/**
	 * add() method must store unique elements only (existing elements must be ignored). 
	 * This will return true if the element was successfully added. 
	 * @param address
	 * @return
	 */

	public boolean add(InetAddress address) {
	  if(Objects.isNull(address)){
	    log.error("address is null, failed to add to queue");
	    return false;
    }
    if(addressQueue.size() == MAX_SIZE){
	    log.error("Queue is full, cannot enqueue");
	    return false;
    }
	  try {
	    AddressNode node = AddressNode.builder()
          .address(address)
          .enqueueTime(LocalDateTime.now())
          .build();
	    //adding it to cache to track the expired addresses
      List<AddressNode> nodeList = addressMap.getOrDefault(address, new ArrayList<>());
      nodeList.add(node);
      addressMap.put(address, nodeList);
      return addressQueue.offer(address);
    } catch (Exception ex){
	    log.error("Exception while adding address: {}", address);
    }
    return false;
	}

	/**
	 * remove() method will return true if the address was successfully removed
	 * @param address
	 * @return
	 */
	public boolean remove(InetAddress address) {
    if(Objects.isNull(address)){
      log.error("address is null, failed to remove from queue");
      return false;
    }
    try {
      //removing from cache also
      List<AddressNode> nodeList = addressMap.getOrDefault(address, new ArrayList<>());
      if(!nodeList.isEmpty()){
        nodeList.remove(0);
        addressMap.put(address, nodeList);
      }
      return addressQueue.remove(address);
    } catch (Exception ex){
      log.error("Exception while removing address: {}", address);
    }
    return false;
	}

	/**
	 * The peek() method will return the most recently added element, 
	 * null if no element exists.
	 * @return
	 */
	public InetAddress peek() {
    try {
      return addressQueue.peek();
    } catch (Exception ex){
      log.error("Exception while peeking address.");
    }
    return null;
	}

	/**
	 * take() method retrieves and removes the most recently added element 
	 * from the cache and waits if necessary until an element becomes available.
	 * @return
	 */
	public InetAddress take() {
    try {
      return getNextActiveAddress();
    } catch (Exception ex){
      log.error("Exception while fetching address.");
    }
    return null;
	}

  /**
   * fetches the next address from queue which has not expired.
   * @return address
   */
	private InetAddress getNextActiveAddress() throws InterruptedException {
	  InetAddress address = null;
	  while(true) {
	    log.debug("get Next Active Address");
      address = addressQueue.take();
      List<AddressNode> nodeList = addressMap.get(address);
      if (nodeList == null || nodeList.isEmpty()) {
        log.error("NodeList empty in cache");
        //if address is not present in map, discarding the address as invalid address.
        continue;
      }
      /*
      If the address has expired, remove it from queue, and fetch next active address
       */
      if (nodeList.get(0).getEnqueueTime().isBefore(LocalDateTime.now().minus(evictionTimeInMillis,
          ChronoUnit.MILLIS))) {
        nodeList.remove(0);
        addressMap.put(address, nodeList);
        log.info("Removing expired address: {}", address);
        continue;
      }
      return address;
    }
  }
	
}