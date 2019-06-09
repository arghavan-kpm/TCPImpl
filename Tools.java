import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.*;

public class Tools{

	public static int convertToUnsigned(byte a){
		return a & 0xff;
	}

	public static byte[] readFromFile(String pathToFile){

		try{
			File file = new File(pathToFile);

			byte[] bytesArray = new byte[(int) file.length()];

			FileInputStream fis = new FileInputStream(file);
			fis.read(bytesArray);
			fis.close();

			return bytesArray;

		}catch(Exception e){}

		return null;
	}

	public static byte[] merge(byte[] a1,byte[] a2){
		byte[] b = new byte[a1.length + a2.length];
		for(int i=0;i < a1.length;i++)
			b[i] = a1[i];
		for(int i=0;i < a2.length;i++)
			b[i + a1.length] = a2[i];
		return b;
	}


	public static ArrayList<DatagramPacket> make_pkt( byte[] data, int pkt_size, InetAddress addr, int port){

		ArrayList<DatagramPacket> res = new ArrayList<DatagramPacket>();
		byte[] Buf = new byte[pkt_size + 4];

		for(int i=0; i< data.length; i+=pkt_size){

			

			Buf = ByteBuffer.allocate(4).putInt(i).array();

			if(data.length - i >=  pkt_size){

				Buf = merge(Buf, new byte[]{0});
				Buf = merge(Buf,Arrays.copyOfRange(data, i, i + pkt_size));
				
			}
			else{
				Buf = merge(Buf, new byte[]{1});
				Buf = merge(Buf, Arrays.copyOfRange(data, i, data.length));
			}

			res.add(new DatagramPacket(Buf,Buf.length,addr,port));
		}

		return res;
	}

	public static void writeToFile(String data,String pathToFile) throws IOException{
		PrintWriter writer = new PrintWriter(pathToFile,"UTF-8");
		writer.print(data);
		writer.close();
	}

}