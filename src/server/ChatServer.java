package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import model.MsgOnlineUsers;
import model.MsgToClient;
import model.MsgToServer;
import model.MsgUserExists;

public class ChatServer extends Application {
	private TextArea taLog = new TextArea();

	private ExecutorService executor;
	private ServerSocket server;
	
	// private Map<String, Socket> onlineUsers = new HashMap<String, Socket>();
	private ObservableMap<String, HandleClient> onlineUsers = FXCollections.observableHashMap();

	@Override
	public void start(Stage stage) throws Exception {
		buildUI(stage);

		stage.show();
		
		new Thread(() -> accept()).start();
		stage.setOnCloseRequest(e -> close());
		registerOnlineUsersList();
	}
	
	private void registerOnlineUsersList() {
		onlineUsers.addListener(new InvalidationListener() {
			@Override
			public void invalidated(Observable o) {
				log("Users list:");
				for(String userName : onlineUsers.keySet()) {
					log("\t" + userName);
				}
			}
		});
	}
	
	private void close() {
		try {
			server.close();
			executor.shutdown();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public void accept() {
		try {
			executor = Executors.newFixedThreadPool(16);
			server = new ServerSocket(5000);

			log("Server started " + "\n");

			while (!executor.isShutdown()) {
				Socket client = server.accept();
				log("New client connected. IP: " + server.getInetAddress().getHostAddress());
				HandleClient thread = new HandleClient(client);
				executor.submit(thread);
			}

		} catch (IOException e) {
			log(e.toString());
		}
	}

	private void buildUI(Stage stage) {
		BorderPane pane = new BorderPane();
		pane.setCenter(taLog);

		taLog.setEditable(false);

		Scene scene = new Scene(pane);

		stage.setScene(scene);
	}

	synchronized public void log(String log) {
		taLog.appendText("# " + log + "\n");
	}

	public static void main(String[] args) {
		launch(args);
	}

	class HandleClient extends Thread {
		private Socket client;
		private ObjectOutputStream out;
		private ObjectInputStream in;
		private String userName;

		public HandleClient(Socket socket) {
			client = socket;
		}
		
		public Socket getSocket() {
			return client;
		}
		
		public ObjectOutputStream getOutputStream() {
			return out;
		}
		
		private void registerUser() throws IOException, ClassNotFoundException {
			while(true) {
				String userName = (String) in.readObject();
				if(onlineUsers.containsKey(userName)) {
					out.writeObject(new MsgUserExists());
				} else {
					onlineUsers.put((String) userName, this);
					sendOnlineUsers();
					this.userName = userName;
					break;
				}
			}
		}
		
		private void acceptMessages() throws ClassNotFoundException, IOException {
			while(client.isConnected() && !executor.isShutdown()){
				Object message = in.readObject();
				
				if(message instanceof MsgToServer) {
					MsgToClient msgToClient = new MsgToClient(((MsgToServer) message).getMsg(), userName);
					out.writeObject(msgToClient);
					for(String user : ((MsgToServer) message).getTo()) {
						if(user.compareTo(userName) != 0) {
							HandleClient clientHandler = onlineUsers.get(user);
							clientHandler.getOutputStream().writeObject(msgToClient);
						}
					}
				}
			}
		}
		
		private void sendOnlineUsers() throws IOException {
			MsgOnlineUsers message = new MsgOnlineUsers(onlineUsers.keySet().toArray(new String[0]));
			out.writeObject(message);
		}

		public void run() {
			InvalidationListener onlineUsersChangedListener = new InvalidationListener() {
				@Override
				public void invalidated(Observable o) {
					try {
						sendOnlineUsers();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			
			try {
				out = new ObjectOutputStream(client.getOutputStream());
				in = new ObjectInputStream(client.getInputStream());
				
				registerUser();
				
				onlineUsers.addListener(onlineUsersChangedListener);
				
				acceptMessages();
			} catch (IOException ex) {
				log(ex.toString());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				onlineUsers.removeListener(onlineUsersChangedListener);
				onlineUsers.remove(userName);
			}
		}
	}
}
