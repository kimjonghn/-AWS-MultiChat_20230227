package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JFrame;

public class ServerApplication {
	
	public static void main(String[] args) {
		JFrame serverFrame = new JFrame("서버");//서버라는 이름을 가진 jframe
		serverFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//창닫으면 종료
		serverFrame.setSize(300, 300);//크기를 300 300
		serverFrame.setVisible(true);//jframe이 보이게 하는 역할
		
		try {
			ServerSocket serverSocket = new ServerSocket(9090);
			
			while(true) {
				Socket socket = serverSocket.accept(); //서버소켓이 요청이 올때가지 대기
				//클라이언트가 연결요청하면 소켓 객체를 반환(연결 요청이 오면 소켓객체를 가지고옴)
				ConnectedSocket connectedSocket = new ConnectedSocket(socket); 
				connectedSocket.start();
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}
 