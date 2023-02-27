package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import dto.request.RequestDto;
import dto.response.ResponseDto;
import lombok.Getter;
@Getter
public class ConnectedSocket extends Thread {
	
	private static List<ConnectedSocket> connectedSocketList = new ArrayList<>();
	private Socket socket;
	private String username;
	
	private Gson gson;
	
	public ConnectedSocket(Socket socket) {
		this.socket = socket;
		gson = new Gson();
	}
	@Override
	public void run() {
		while(true) {
			BufferedReader bufferedReader;
			try {
				bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String requestJson = bufferedReader.readLine();
				
				System.out.println("요청: " + requestJson);
				requestMapping(requestJson);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	private void requestMapping(String requestJson) {
		RequestDto<?> requestDto = gson.fromJson(requestJson, RequestDto.class);
		
		switch(requestDto.getResource()) {
			case "usernameCheck":
				checkUsername((String) requestDto.getBody());
				break;
		}
	}
	private void checkUsername(String username) {
			if(username.isBlank()) {
				sendToMe(new RequestDto<String>("usernameCheckIsBlank", "사용자 이름은 공백일 수 없습니다."));
				return;
			}
		for(ConnectedSocket connectedSocket : connectedSocketList) {
			if(connectedSocket.getUsername().equals(username)) {
				sendToMe(new RequestDto<String>("usernameCheckIsDuplicate", "이미 사용중인 이름입니다."));
				return;
			}
		}
		this.username = username;
		connectedSocketList.add(this);
		sendToMe(new ResponseDto<String>("usernameCheckSuccessfully", null));
	}
	private void sendToMe(ResponseDto<?> responseDto) {
		try {
			OutputStream outputStream = socket.getOutputStream();
			PrintWriter printWriter = new PrintWriter(outputStream, true);
			
			String responseJson = gson.toJson(responseDto);
			printWriter.println(responseJson);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void sendToAll() {
		
	}
}
