package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import dto.request.RequestDto;
import dto.response.ResponseDto;
import entity.Room;
import lombok.Getter;
@Getter
public class ConnectedSocket extends Thread {
	
	private static List<ConnectedSocket> connectedSocketList = new ArrayList<>();
	private static List<Room> roomList = new ArrayList<>();
	public static int index = 0;
	private Socket socket;
	private String username;
	
	private Gson gson;
	
	public ConnectedSocket(Socket socket) {//serverSocket
		this.socket = socket;
		gson = new Gson();
		Room room = new Room("TestRoom" + index, "TestUser" + index);
		index++;
		roomList.add(room);
	}
	@Override
	public void run() {
		while(true) {
			BufferedReader bufferedReader;//연결된 소켓에서 데이터를 읽기위해 객체 생성 코드
			try {
				bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				//socket.getInputStream() 메서드는 소켓으로 들어오는 입력 스트림(InputStream) 객체를 반환합니다.
				String requestJson = bufferedReader.readLine();//readLine()메서드를 이용해 한줄씩 데이터를 읽을수 있다
				
				System.out.println("요청: " + requestJson);//json형식으로 바꾼 데이터
				requestMapping(requestJson);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	private void requestMapping(String requestJson) {
		RequestDto<?> requestDto = gson.fromJson(requestJson, RequestDto.class);
		//json형식의 문자열을 파싱하여 RequestDto객체로 변환하는 코드
		switch(requestDto.getResource()) {//witch문을 이용하여 requestDto에서 추출한 요청 리소스에 따라 다른 메서드를 호출합니다.
			case "usernameCheck"://client에서 usernameCheck이라는 요청을 했을때 지금 case문이 실행된다
				checkUsername((String) requestDto.getBody());
				//checkUsername() 메서드는 requestDto 객체의 body(이름) 필드에 담긴 데이터를 인자로 받아 처리합니다
				break;
			case "createRoom":
				Room room = new Room((String) requestDto.getBody(), username);
				room.getUsers().add(this);
				roomList.add(room);
				ResponseDto<String> responseDto = new ResponseDto<String>("createRoomSuccessfully", null);
				sendToMe(responseDto);
				refreshUsernameList(username);
				sendToAll(refreshRoomList(), connectedSocketList);
				break;
		}
	}
	private void checkUsername(String username) {//usernameCheck case문 안에 getBody로 받은 문자를 받는다
			if(username.isBlank()) {//만약 공백일 경우 실행이된다
				sendToMe(new ResponseDto<String>("usernameCheckIsBlank", "사용자 이름은 공백일 수 없습니다."));
				return;
			}
		for(ConnectedSocket connectedSocket : connectedSocketList) {
			if(connectedSocket.getUsername().equals(username)) {//중복되는게 있는지 검사해준다
				sendToMe(new ResponseDto<String>("usernameCheckIsDuplicate", "이미 사용중인 이름입니다."));
				return;
			}
		}
		this.username = username;//connectedSocket username을 this.username에 저장
		connectedSocketList.add(this);  //connectedSocketList에 suername을 추가
		sendToMe(new ResponseDto<String>("usernameCheckSuccessfully", null));
		//sendToMe()이 메서드는 해당 클라이언트에게 ResponseDto 객체를 보내는 역할을 합니다.
		sendToMe(refreshRoomList());
		//방리스트 초기화
	}
	private ResponseDto<List<Map<String,String>>> refreshRoomList() {
		List<Map<String,String>> roomNameList = new ArrayList<>();
		
		for(Room room : roomList) {
			Map<String, String> roomInfo = new HashMap<>();
			roomInfo.put("roomName", room.getRoomName());
			roomInfo.put("owner", room.getOwner());
			roomNameList.add(roomInfo);
		}
		ResponseDto <List<Map<String,String>>> responseDto = new ResponseDto<List<Map<String,String>>> ("refreshRoomList", roomNameList);
		return responseDto;
	}
	
	private Room findConnectedRoom(String username) {
		for(Room r : roomList) {
			for(ConnectedSocket cs : r.getUsers()) {
				if(cs.getUsername().equals(username)) {
					return r;
				}
			}
		}
		return null;
	}
	
	private Room findRoom(Map<String, String> roomInfo) {
		for(Room room : roomList) {
			if(room.getRoomName().equals(roomInfo.get("roomName")) && room.getOwner().equals(roomInfo.get("owner"))) {
				return room;
			}
		}
		return null;
	}
	
	private void refreshUsernameList(String usernmae){
		Room room = findConnectedRoom(usernmae);
		List<String> usernameList = new ArrayList<>();
		usernameList.add("방제목: " + room.getRoomName());
		for(ConnectedSocket connectedSocket : room.getUsers()) {
			if(connectedSocket.getUsername().equals(room.getOwner())) {
				usernameList.add(connectedSocket.getUsername() + "(방장)");
				continue;
			}
			usernameList.add(connectedSocket.getUsername());
		}
		ResponseDto<List<String>> responseDto = new ResponseDto<List<String>>("refreshUsernameList", usernameList);
		sendToAll(responseDto, room.getUsers());
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
	private void sendToAll(ResponseDto<?> responseDto, List<ConnectedSocket> connectedSockets) {
		for(ConnectedSocket connectedSocket : connectedSockets) {
			try {
				OutputStream outputStream = connectedSocket.getSocket().getOutputStream();
				PrintWriter printWriter = new PrintWriter(outputStream, true);
			
				String responseJson = gson.toJson(responseDto);
				printWriter.println(responseJson);
			
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
