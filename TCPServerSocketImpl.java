import java.util.Timer;
import java.net.*;
import java.util.TimerTask;

public class TCPServerSocketImpl extends TCPServerSocket {
    

    private byte[] buf = new byte[1408]; 
    private static String STATE = "";
    protected static InetAddress address;
    protected static int port;



    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
        this.port = port;

    }

    @Override
    public TCPSocket accept() throws Exception {

        DatagramPacket packet = new DatagramPacket(this.buf, this.buf.length);

        String received = "";

        String msg = "";

        System.err.println("Waiting for Connection ... ");

        do{
            
            socket.receive(packet);

            this.address = packet.getAddress();
            this.port = packet.getPort();
            packet = new DatagramPacket(this.buf, this.buf.length, this.address, this.port);
            received = new String(packet.getData(), 0, packet.getLength());
            
        }while(received.substring(0,3).equals("SYN") == false);
        
                    System.err.println("Connection request from : " + this.address.getHostAddress() + "\t port: " + String.format("%d", this.port));

        STATE = "SYN-RECEIVED";

                    System.err.println("STATE : " + STATE);


        received = "";
        do{
            System.err.println("Sending SYN-ACK ... ");
          
            String Msg = "SYN-ACK";
            buf = Msg.getBytes();
            DatagramPacket Packet = new DatagramPacket(buf, buf.length, address, port);
            
            try{
                socket.send(Packet);
            }catch(Exception e){}
            
            buf = new byte[1408];
            Packet = new DatagramPacket(buf, buf.length);
            
            try{
                socket.setSoTimeout(1000);
                socket.receive(Packet);
                received = new String(Packet.getData(), 0, Packet.getLength());
            }catch(Exception e){}



        }while(received.equals("") || received.substring(0,3).equals("ACK") == false);

        System.err.println("ACK received...");

        STATE = "ESTABLISHED";
        System.err.println("-----------------------------------------------------------------------------------");

        TCPSocketImpl tcpSocket = new TCPSocketImpl(address.getHostAddress(), this.port);
        tcpSocket.StateSetter("ESTABLISHED");
        tcpSocket.SetSocket(this.socket);
        return tcpSocket;
    }

    @Override
    public void close() throws Exception {
        socket.close();

    //    throw new RuntimeException("Not implemented!");
    }
}
