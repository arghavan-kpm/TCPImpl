import java.util.*;
import java.net.*;
import java.nio.*;

public class TCPSocketImpl extends TCPSocket {

    private EnhancedDatagramSocket socket;
    private String STATE = "";
    private byte[] buf = new byte[1408];
    private InetAddress address ;
    private long CWND = 1; 
    private long ssThresh = 8;

    private int pkt_size = 300;

    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
        address =  InetAddress.getByName(IP);
        socket = new EnhancedDatagramSocket(0);
    }

    
    public void StateSetter(String s){
        STATE = s;
    }

    public void SetSocket(EnhancedDatagramSocket socket){
        this.socket = socket;
    }

    private void connect(){
        String msg = "SYN";
        DatagramPacket packet = null;
        STATE = "SYN-SENT";
        String received = "";

        do{
            System.err.println("Sending Connection request ...");
            buf = new byte[1408];
            buf = msg.getBytes();
            packet = new DatagramPacket(buf, buf.length, address, PORT);
          
            try{
                socket.send(packet); 
            }catch(Exception e){}

            buf = new byte[1408];
            packet = new DatagramPacket(buf, buf.length);
          
            try{
                socket.setSoTimeout(1000);
                socket.receive(packet);
                received = new String(packet.getData(), 0, packet.getLength());
            }catch(Exception e){}

        }while(received.equals("") || received.substring(0,7).equals("SYN-ACK") == false);

        STATE = "ESTABLISHED";
        
        System.err.println("SYN-ACK received ... ");
        
        msg = "ACK";
        buf = new byte[1408];
        buf = msg.getBytes();
        packet = new DatagramPacket(buf, buf.length, address, PORT);
        System.err.println("Sending ACK ...");
        try{
            socket.send(packet);
        }catch(Exception e){}

        System.err.println("--------------------------------------------------------------------------------------");

    }

    @Override
    public void send(String pathToFile) throws Exception {
                        /* 3 WAY HANDSHAKE */ 
        connect();
// ----------------------------------------------------------------------------------------------------------------------
        long Base = 0;

        byte[] Data = Tools.readFromFile(pathToFile);

        ArrayList<DatagramPacket> Pkts = Tools.make_pkt(Data, pkt_size,  address, this.PORT);

        int timer = 1000;
        int LastSent = 0;

        int tahoe = 0;

        while( Base < Pkts.size() ){            
            buf = new byte[1408];
            DatagramPacket ackPkt = new DatagramPacket(buf,buf.length);

            for(int i=LastSent;i < Base+CWND && i < Pkts.size();i++){
                socket.send(Pkts.get(i));
                System.err.println("Seg# " + String.format("%d",i) + " sent");
            }
            
            LastSent = (int)(Base + CWND);

            try{
                socket.setSoTimeout(timer);
                socket.receive(ackPkt);
                if( Base == 0 && ackPkt.getData()[0] == 83 ){       // received a SYN_ACK
                    
                    String msg = "ACK";
                    buf = new byte[1408];
                    buf = msg.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, PORT);
                    System.err.println("Sending ACK ...");
                    try{
                        socket.send(packet);
                    }catch(Exception e){}
                    continue;

                }
                long ackNum = Tools.convertToUnsigned(ackPkt.getData()[0]) * (1 << 24) + Tools.convertToUnsigned(ackPkt.getData()[1]) * (1 << 16) + Tools.convertToUnsigned(ackPkt.getData()[2]) * (1 << 8) + Tools.convertToUnsigned(ackPkt.getData()[3]);

                System.err.println("Ack# " + String.format("%d" , ackNum) + " received");

                ackNum /= pkt_size;
                
                if(ackNum >= Base){
                    if(CWND < ssThresh){
                        CWND += ackNum - Base + 1;
                        onWindowChange();
                    }
                    else if(ackNum == LastSent){
                        CWND += 1;
                        onWindowChange();
                    }
                    Base = ackNum + 1;
                }
                else
                    tahoe++;

                System.err.println("new window base : " + String.format("%d", Base));

            }catch(Exception e){
                tahoe = 3;
                System.err.println(" ---Timeout--- ");
                LastSent = (int)Base;

            }
            
            if(tahoe == 3){
                LastSent = (int)Base;
                //socket.send(Pkts.get((int)Base));
                ssThresh = CWND / 2;
                CWND = 1;
                tahoe = 0;
                onWindowChange();
            }
        }
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        String cache = "";
        buf = new byte[1408];
        String msg = "ACK";
        String received="";
        int expected = 0;

        byte isFinished = 0;
        do{
            buf = new byte[1408];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            socket.setSoTimeout(0);
            socket.receive(packet);
            received = new String(packet.getData(), 0, packet.getLength());

            System.err.println( String.format("%d",received.length()) + " byte data received ");

            buf = new byte[]{packet.getData()[0],packet.getData()[1],packet.getData()[2],packet.getData()[3]};
          
            int pktNum = Tools.convertToUnsigned(buf[0]) * (1 << 24 ) + Tools.convertToUnsigned(buf[1]) * (1 << 16) + Tools.convertToUnsigned(buf[2]) * (1 << 8) + Tools.convertToUnsigned(buf[3]);

            System.err.println("seq # " + String.format("%d", pktNum) + " received, Expected : " + String.format("%d" , expected ));

            if(pktNum == expected){
                isFinished = packet.getData()[4];
                expected += pkt_size;
                cache += received.substring(5);  
            }
            
            buf = ByteBuffer.allocate(4).putInt(expected -pkt_size ).array();
            
            if(expected == 0)
                buf = ByteBuffer.allocate(4).putInt(0).array();

            

            buf = Tools.merge(buf,msg.getBytes());

            packet = new DatagramPacket(buf, buf.length, address, this.PORT);
            socket.send(packet);


            System.err.println("ACK # " + String.format("%d", expected - pkt_size) + " sent");            


        }while( isFinished == 0 );

        Tools.writeToFile(cache,pathToFile);

        buf = ByteBuffer.allocate(4).putInt(expected -pkt_size ).array();
        buf = Tools.merge(buf,msg.getBytes());
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, this.PORT);
        for(int i=0;i < 20;i++)
            socket.send(packet);

        //System.out.println(received);

    }

    @Override
    public void close() throws Exception {
       socket.close();
       // throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getSSThreshold() {
        return ssThresh * pkt_size;
    }

    @Override
    public long getWindowSize() {
        return CWND * pkt_size;
    }
}
