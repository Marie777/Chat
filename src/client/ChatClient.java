package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.MsgOnlineUsers;
import model.MsgToClient;
import model.MsgToServer;
import model.MsgUserExists;

public class ChatClient extends Application {

	private Scene sceneChat;
	private TextArea taViewMsg = new TextArea();
	private Label lblUserName = new Label("User Name:");
	private TextField tfNewMsg = new TextField();
	private Button btnSend = new Button("Send");
	private ListView<String> lvOnlineUsers = new ListView<String>();
	private Scene sceneRegistration;
	private TextField tfRegistration = new TextField();
	private Button btnOk = new Button("Ok");
	private Label lblRegistartionError = new Label();
	private Stage stage;
	
	private ObservableList<String> onlineUsersList = FXCollections.observableArrayList();
	
	Socket socket;
	ObjectInputStream in;
	ObjectOutputStream out;
	
	@Override
	public void start(Stage stage) throws Exception {
		this.stage = stage;
		
		socket = new Socket("localhost", 5000);
		in = new ObjectInputStream(socket.getInputStream());
		out = new ObjectOutputStream(socket.getOutputStream());
		
		buildUI();
		buildBehavior();
		
		stage.setScene(sceneRegistration);
		stage.show();
	}
	
	public void buildBehavior() {
		lvOnlineUsers.setItems(onlineUsersList);
		
		stage.setOnCloseRequest(e -> {
			try {
				socket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});
		
		btnOk.setOnAction(e -> register());
		tfRegistration.setOnAction(e -> register());
		
		tfNewMsg.setOnAction(e -> sendMessage(tfNewMsg.getText()));
		btnSend.setOnAction(e -> sendMessage(tfNewMsg.getText()));
	}
	
	private void sendMessage(String msgText) {
		ObservableList<String> sendTo = lvOnlineUsers.getSelectionModel().getSelectedItems();
		MsgToServer message = new MsgToServer(msgText, sendTo.toArray(new String[0]));
		tfNewMsg.setText("");
		
		try {
			out.writeObject(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void register() {
		try {
			String userName = tfRegistration.getText();
			out.writeObject(userName);
			Object message = in.readObject();
			if(message instanceof MsgUserExists) {
				lblRegistartionError.setText("Username already exists!");
			} else if(message instanceof MsgOnlineUsers) {
				lblUserName.setText(userName + ":");
				stage.setScene(sceneChat);
				
				onlineUsersList.clear();
				onlineUsersList.addAll(((MsgOnlineUsers) message).getUsers());
				
				Thread t = new ServerListener(in, onlineUsersList);
				t.start();
			}
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void buildUI(){
		HBox newMsgPane = new HBox();
		newMsgPane.getChildren().addAll(lblUserName, tfNewMsg, btnSend);
		newMsgPane.setAlignment(Pos.BASELINE_CENTER);
		
		HBox.setHgrow(lblUserName, Priority.NEVER);
		HBox.setHgrow(tfNewMsg, Priority.ALWAYS);
		HBox.setHgrow(btnSend, Priority.NEVER);
		
		HBox.setMargin(lblUserName, new Insets(0, 10, 0, 10));
		
		lvOnlineUsers.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		taViewMsg.setEditable(false);
		
		BorderPane mainPane = new BorderPane();
		mainPane.setBottom(newMsgPane);
		mainPane.setRight(lvOnlineUsers);
		mainPane.setCenter(taViewMsg);
		
		sceneChat = new Scene(mainPane);
		
		Label lblRegistration = new Label("User Name:");
		lblRegistartionError.setTextFill(Color.RED);
		BorderPane registrationPane = new BorderPane();
		
		HBox registrationPaneHbox = new HBox();
		HBox.setMargin(lblRegistration, new Insets(0, 10, 0, 10));
		registrationPaneHbox.getChildren().addAll(lblRegistration, tfRegistration, btnOk);
		registrationPaneHbox.setAlignment(Pos.CENTER);
		
		registrationPane.setCenter(registrationPaneHbox);
		registrationPane.setBottom(lblRegistartionError);
		
		sceneRegistration = new Scene(registrationPane);
	}

	public static void main(String[] args) {
		launch(args);
	}
	
	class ServerListener extends Thread {
		private ObjectInputStream in;
		private ObservableList<String> onlineUsersList;
		
		public ServerListener(ObjectInputStream in, ObservableList<String> onlineUsersList) {
			this.in = in;
			this.onlineUsersList = onlineUsersList;
		}
		
		@Override
		public void run() {
			while(true) {
				try {
					Object message = in.readObject();
					
					if(message instanceof MsgOnlineUsers) {
						Platform.runLater(() -> {
							onlineUsersList.clear();
							onlineUsersList.addAll(((MsgOnlineUsers) message).getUsers());
						});
					} else if(message instanceof MsgToClient) {
						MsgToClient msgToCLient = (MsgToClient) message;
						Platform.runLater(() -> {
							taViewMsg.appendText(msgToCLient.getFrom() + ": " + msgToCLient.getMsg() + "\n");
						});
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					break;
				}
			}
		}
	}
}
