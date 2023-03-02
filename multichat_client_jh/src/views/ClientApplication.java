package views;

import java.awt.CardLayout;
import java.awt.EventQueue;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.google.gson.Gson;

import dto.request.RequestDto;
import lombok.Getter;
import lombok.Setter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
@Getter
public class ClientApplication extends JFrame {

	private static final long serialVersionUID = -4753767777928836759L;
	private static ClientApplication instance;//싱글톤
	
	private Gson gson;
	private Socket socket;
	
	private JPanel mainPanel;
	private CardLayout mainCard;
	
	private JTextField usernameField;
	
	private JTextField sendMessageField;
	
	@Setter
	private List<Map<String,String>> roomInfoList;
	private DefaultListModel<String> roomNameListModel;
	private DefaultListModel<String> usernamelistModel;
	private JList roomList;
	private JList joinUserList;
	private JTextArea chattingContent;
	
	public static ClientApplication getInstance() {
		if(instance == null) {
			instance = new ClientApplication();
		}
		return instance;
	}
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ClientApplication frame = ClientApplication.getInstance();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private ClientApplication() {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				RequestDto<String> requestDto = new RequestDto<String>("exitRoom", null);
				sendRequest(requestDto);
			}
		});
		
		/*========<< init >>========*/
		gson = new Gson();
		try {
			socket = new Socket("127.0.0.1", 9090);
			ClientRecive clientRecive = new ClientRecive(socket);
			clientRecive.start();
			
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (ConnectException e1) {
			JOptionPane.showMessageDialog(this, "서버에 접속할 수 없습니다.", "접속오류", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		/*========<< frame set >>========*/
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(600, 150, 480, 800);
		
		/*========<< panel >>========*/
		
		mainPanel = new JPanel();
		JPanel loginPanel = new JPanel();
		JPanel roomListPanel = new JPanel();
		JPanel roomPanel = new JPanel();
		
		
		/*========<< layout >>========*/
		mainCard = new CardLayout();
		
		mainPanel.setLayout(mainCard);
		loginPanel.setLayout(null);
		roomListPanel.setLayout(null);
		roomPanel.setLayout(null);
		
		
		/*========<< panel set >>========*/
		
		setContentPane(mainPanel);
		mainPanel.add(loginPanel, "loginPanel");
		mainPanel.add(roomListPanel, "roomListPanel");
		mainPanel.add(roomPanel, "roomPanel");
		
		
		JButton enterButton = new JButton("접속하기");
		/*========<< login panel >>========*/
		
		usernameField = new JTextField();
		usernameField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					RequestDto<String> usernameCheckReqDto = 
							new RequestDto<String>("usernameCheck", usernameField.getText());
					sendRequest(usernameCheckReqDto);
					//RequestDto에 (resource) usernameCheck은 요청유형을 식별하는 역할이고 
					//(body) usernameField.getText는 사용자이름을 사용가능한지 확인
					//sendRequest메서드를 호출하고 요청
				}
			}
		});
		
		usernameField.setBounds(59, 428, 324, 45);
		loginPanel.add(usernameField);
		usernameField.setColumns(10);
		
		enterButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				RequestDto<String> usernameCheckReqDto = 
						new RequestDto<String>("usernameCheck", usernameField.getText());
				sendRequest(usernameCheckReqDto);
			}
		});
		enterButton.setBounds(59, 483, 324, 45);
		loginPanel.add(enterButton);
		
		
		/*========<< roomList panel >>========*/
		
		JScrollPane roomListScroll = new JScrollPane();
		roomListScroll.setBounds(117, 0, 337, 751);
		roomListPanel.add(roomListScroll);
		
		roomNameListModel = new DefaultListModel<String>(); //?
		roomList = new JList(roomNameListModel); // ?
		roomList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2) {//roomlist에서 더블클릭을 했을때 
					int selectedIndex = roomList.getSelectedIndex();
					RequestDto<Map<String, String>> requestDto = 
							new RequestDto<Map<String, String>>("enterRoom", roomInfoList.get(selectedIndex));
					sendRequest(requestDto);
					//	RequestDto객체를 생성하여"enterRoom"은 요청의 유형을 식별하는데 사용
					//roomInfoList.get(selectedIndex)는 Map<String, String>객체로 표현되며 Json형식으로 인코딩
					//sendRequest에 요청
				}
			}
		});
		roomListScroll.setViewportView(roomList);
		
		JButton createRoomButton = new JButton("방생성");
		createRoomButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				String roomName = null;
				while(true) {
					roomName = JOptionPane.showInputDialog(null, "생성할 방의 제목을 입력하세요","방생성",JOptionPane.PLAIN_MESSAGE);
					if(roomName == null) {
						return;
					}//x버튼을 눌러서 나갈경우 return을 한다
					if(!roomName.isBlank()) {
						break;
					}//공백이 아닐경우 break걸려 while문을 종료
					JOptionPane.showMessageDialog(null, "공백은 사용할 수 없습니다","방생성 오류", JOptionPane.ERROR_MESSAGE);//공백일 경우
					}
				RequestDto<String> requestDto = new RequestDto<String>("createRoom", roomName);
				sendRequest(requestDto);
				//RequestDto에 creatRoom이라는 요청의 유형을 식벽하게 하는 문자를 resourse에 답고  roomName을 body에 저장
				}
		});
		createRoomButton.setBounds(8, 10, 97, 96);
		roomListPanel.add(createRoomButton);
		
		
		/*========<< room panel >>========*/
	
		JScrollPane joinUserListScroll = new JScrollPane();
		joinUserListScroll.setBounds(0, 0, 348, 100);
		roomPanel.add(joinUserListScroll);
		
		usernamelistModel = new DefaultListModel<String>(); //?
		joinUserList = new JList(usernamelistModel);
		joinUserListScroll.setViewportView(joinUserList);
		
		JButton roomExitButton = new JButton("나가기");
		roomExitButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				//0 = yes를 선택했을때
				if(JOptionPane.showConfirmDialog(null, "정말로 방을 나가시겠습니까?", "방 나가기", JOptionPane.YES_NO_OPTION) == 0) {
					RequestDto<String> requestDto = new RequestDto<String>("exitRoom", null);
					sendRequest(requestDto);					
				}
			}
		});
		roomExitButton.setBounds(348, 0, 106, 100);
		roomPanel.add(roomExitButton);
		
		JScrollPane chattingContentScroll = new JScrollPane();
		chattingContentScroll.setBounds(0, 98, 454, 596);
		roomPanel.add(chattingContentScroll);
		
		chattingContent = new JTextArea();
		chattingContentScroll.setViewportView(chattingContent);
		chattingContent.setEditable(false);
		
		sendMessageField = new JTextField();
		sendMessageField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					RequestDto<String> requestDto = new RequestDto<String>("sendMessage", sendMessageField.getText());
					sendRequest(requestDto);
				}
			}
		});
		sendMessageField.setBounds(0, 694, 381, 57);
		roomPanel.add(sendMessageField);
		sendMessageField.setColumns(10);
		
		JButton sendButton = new JButton("전송");
		sendButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				RequestDto<String> requestDto = new RequestDto<String>("sendMessage", sendMessageField.getText());
				sendMessageField.setText("");//enter쳤을때 메세지 비워지게
				sendRequest(requestDto);
			}
		});
		sendButton.setBounds(380, 694, 74, 57);
		roomPanel.add(sendButton);
		
	}
	
	private void sendRequest(RequestDto<?> requestDto) {
		String reqJson = gson.toJson(requestDto); //서버로 보내기위해 json형태로 변형
		OutputStream outputStream = null; //서버로 보내기 위해 생성
		PrintWriter printWriter = null;
		try {
			outputStream = socket.getOutputStream();
			printWriter = new PrintWriter(socket.getOutputStream(), true);
			printWriter.println(reqJson); 
			System.out.println("클라이언트 -> 서버: " + reqJson);
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
}
