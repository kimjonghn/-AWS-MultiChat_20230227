package views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

import javax.swing.JOptionPane;

import com.google.gson.Gson;

import dto.response.ResponseDto;

public class ClientRecive extends Thread{
	
	private Socket socket;
	private Gson gson;
	
	public ClientRecive(Socket socket) {
		this.socket = socket;
		gson = new Gson();
	}
	
	@Override
	public void run() {
		
		try {
			InputStream inputStream = socket.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			while(true) {
				String responseJson = bufferedReader.readLine();
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void responseMapping(String responseJson) {
		ResponseDto<?> responseDto = gson.fromJson(responseJson, ResponseDto.class);
		switch(responseDto.getResource()) {
		case "usernameCheckIsBlank":// (or) usernameCheckIsDuplicate
		case "usernameCheckIsDuplicate":
			JOptionPane.showMessageDialog(null, (String) responseDto.getBody(), "접속오류", JOptionPane.WARNING_MESSAGE);
			break;
		default:			
			break;
		
		}
	}
	
}
