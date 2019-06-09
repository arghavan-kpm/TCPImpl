public abstract class TCPServerSocket {

	protected EnhancedDatagramSocket socket;


    public TCPServerSocket(int port) throws Exception {
    	socket = new EnhancedDatagramSocket(port);
    }

    public abstract TCPSocket accept() throws Exception;

    public abstract void close() throws Exception;
}