package com.gmail.matejpesl1.mc_cheat_detection;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.regex.Pattern;

import com.gmail.matejpesl1.mc_cheat_detection.Inspection;
import com.gmail.matejpesl1.servers.Basicland;
import com.gmail.matejpesl1.servers.Debug;
import com.gmail.matejpesl1.servers.Server;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;


	public class Main extends Application {
	public static enum Mode {DEBUG, BASICLAND};
	public static final Mode mode = Mode.BASICLAND;
	public static final float PROGRAM_VERSION = 3.9f;
	
	private static final short W_WIDTH = 510;
	private static final short W_HEIGHT = 210;
	private static final short IMG_SIZE = 75;
	private static final short DEFAULT_IMG_Y = (W_HEIGHT/2 - IMG_SIZE/2) + 50;
	private static final short IMGS_OFFSET = 100;
	private static final short LOGO_SIZE = IMG_SIZE + 50;
	
	public static final byte MAX_NAME_LENGTH = 16;
	public static final byte MIN_NAME_LENGTH = 3;
	public static final Pattern UNALLOWED_NAME_CHARACTERS = Pattern.compile("[$&+,:;=\\\\?@#|/'<>.^*() %!-]");
	
	public static ImageView checkMark;
	public static ImageView xMark;
	public static ImageView exit;
	public static ImageView programLogo;
	public static ImageView retry;
	public static ImageView updateArrow;
	
	private static boolean inspectionRunning = false;
	public static enum Requirement{INTERNET, MINECRAFT_DIR, VERSIONS_DIR};
	public static Stage stage;
	public static Inspection inspection;
	public static ArrayList<String> globalErrors = new ArrayList<>();
	private Server currentServer;
	private boolean updateAvailable;
	private boolean splashIsShown;
	public static final boolean DOWNLOAD_FILES_IN_DEBUG = false;
	private static final Color BACKGROUND_COLOR = new Color(0.5, 0.140, 0.925,0.95);
	
	public static void main(String[] args) {
		System.out.println("start");
		setUncatchedExceptionHandler();
		launch(args);
	}
	
	public static void setUncatchedExceptionHandler() {
		try {
			Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			    public void uncaughtException(Thread t, Throwable te) {
			    	te.printStackTrace();
			    	String teStackTrace = te.toString() + "\n";
			    	StackTraceElement[] trace = te.getStackTrace();
			    	for (short i = 0; i < trace.length; ++i) {
			    		teStackTrace += trace[i].toString() + "\n";
			    	}
			    	Exception e = new Exception(teStackTrace);
			    	Inspection.interruptInspection("neo�et�en� vyj�mka", true, e, true);
			    }
			});
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	@Override
	public void start(Stage stage) {
		currentServer = determineServer(mode);
		Thread tSplash = null;
		try {
			tSplash = new Thread(getSplashScreen(0));
			tSplash.start();
			splashIsShown = true;
		} catch (Exception e) {
			e.printStackTrace();
			globalErrors.add("Nepoda�ilo se vytvo�it splash screen.");
		}
		
		Main.stage = stage;
		loadResources();
		prepareGUI();
		preloadProgram(tSplash);
	}
	
	public void preloadProgram(Thread tSplash) {
		UpdateManager updateManagerTemp = null;
		
			try {
				updateManagerTemp = new UpdateManager(this);
				if (!(mode == Mode.DEBUG && !DOWNLOAD_FILES_IN_DEBUG)) {
					updateAvailable = updateManagerTemp.checkUpdateAvailability();	
				}
			} catch (URISyntaxException e) {
				String error = "Cesta k programu nen� spr�vn�.";
				handleErrorInUpdateProcess(error, e);
			} catch (MalformedURLException e) {
				String error = "Nelze na��st odkazy na soubory.";
				handleErrorInUpdateProcess(error, e);
			} catch (Exception e) {
				String error = "Nelze st�hnout soubor s nejnov�j�� verz� programu.";
				handleErrorInUpdateProcess(error, e);
			}

		UpdateManager updateManager = updateManagerTemp;
		updateManagerTemp = null;
			
		//turn off splash
		if (splashIsShown) {
			synchronized(tSplash) {
				tSplash.interrupt();
				System.out.println("tried to interrup from Main");
			}
			splashIsShown = false;
		}
		
		if (updateAvailable) {
			showUpdateScreen();
			new Thread() {
			    public void run() {
					String error = update(updateManager);
					if (error != null) {
						handleErrorInUpdateProcess(error, null);
						checkConditionsAndStartProgram();
					}
			    }
			}.start();
		} else {
			checkConditionsAndStartProgram();	
		}
	}
	
	public void checkConditionsAndStartProgram() {
		Requirement unfulfilledCondition = getUnfulfilledCondition();
		if (unfulfilledCondition == null) {
			startProgram();
		} else {
			showUnfulfilledConditionScreen(unfulfilledCondition);
		}
	}
	
	public void startProgram() {
		inspection = new Inspection(this);
		inspection.start();
		//just a workaround
		stage.setScene(new Scene(new BorderPane(), W_WIDTH, W_HEIGHT));
		stage.show();
		//end of workaround
		showAgreementScreen();
	}
	
	private void loadResources() {
		programLogo = getInternalImage("/resources/program_icons/256x256.png");
		checkMark = getInternalImage("/resources/UI/checkmark.png");
		xMark = getInternalImage("/resources/UI/xmark.png");
		exit = getInternalImage("/resources/UI/exit.png");
		retry = getInternalImage("/resources/UI/retry.png");
		updateArrow = getInternalImage("/resources/UI/update-arrow.png");
	}
	
	private ImageView getInternalImage(String pathToImage) {
		return new ImageView(new Image(getInternalFile(pathToImage).toString()));
	}

	 public static URL getInternalFile(String path) {
		 return Main.class.getResource(path);
	 }
	
	private SplashScreen getSplashScreen(int duration) throws Exception {
		return new SplashScreen(currentServer.getBorderColor(), currentServer.getLogo(), duration, this);
	}
	
	private Server determineServer(Mode rezim) {
		Server currentServer = null;
		switch (rezim) {
			case DEBUG: {
				currentServer = new Debug();
			} break;
			case BASICLAND: {
				currentServer = new Basicland();
			} break;
		}
		return currentServer;
	}
	
	public void showInspectionRunningScreen(String state) {
		Text txtProgress = new Text(10, 35, state);
      
	    programLogo.setX((W_WIDTH/2 - LOGO_SIZE/2));
        programLogo.setY(W_HEIGHT - LOGO_SIZE - 10);
        
        programLogo.setFitHeight(LOGO_SIZE);
        programLogo.setFitWidth(LOGO_SIZE);
        
        txtProgress.setFont(Font.font("Verdana", 19));
        txtProgress.setTextAlignment(TextAlignment.CENTER);
        txtProgress.setWrappingWidth(W_WIDTH - 10);
        
        BorderPane pane = new BorderPane();
        pane.setBackground(getDefaultBackground());
        pane.getChildren().addAll(txtProgress, programLogo);
        
        Scene scene = new Scene(pane, W_WIDTH, W_HEIGHT);
        
        stage.setScene(scene);
	}
	
	public void changeInspectionState(String state) {
		showInspectionRunningScreen(state);
	}
	
	private void showUpdateScreen() {
        Text txtProgress = new Text(10, 35, "Byla nalezena nov� verze! Prob�h� stahov�n� a program bude restartov�n...");
        txtProgress.setFont(Font.font("Verdana", 19));
        txtProgress.setTextAlignment(TextAlignment.CENTER);
        txtProgress.setWrappingWidth(W_WIDTH - 10);

        updateArrow.setX((W_WIDTH/2 - LOGO_SIZE/2));
        updateArrow.setY(W_HEIGHT - LOGO_SIZE - 10);
        
        updateArrow.setFitHeight(LOGO_SIZE);
        updateArrow.setFitWidth(LOGO_SIZE);
        
		BorderPane pane = new BorderPane();
		pane.setBackground(getDefaultBackground());
		pane.getChildren().addAll(txtProgress, updateArrow);
		
		Scene scene = new Scene(pane, W_WIDTH, W_HEIGHT);
		stage.setScene(scene);
		stage.show();
	}
	
	private String update(UpdateManager updateManager) {
		try {
			updateManager.downloadUpdate();
		} catch (IOException e) {
			return "Nepoda�ilo se st�hnout novou verzi.";
		}
		
		try {
			updateManager.update();	
		} catch (IOException e) {
			return "Proces aktualizace se nezda�il.";
		}
		return null;
	}
	
	private void showErrorAlert(String title, String error, Exception e) {
		if (e != null) {
			e.printStackTrace();
		}
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle(title == null ? "Chyba" : title);
		alert.setHeaderText("Chyba: " + error);
		alert.showAndWait();
	}
	
	private void handleErrorInUpdateProcess(String error, Exception e) {
		System.out.println(error);
		globalErrors.add(error);
		showErrorAlert("Chyba p�i aktualizaci", error + " Pokra�uji se starou verz� programu...", e);
	}
	
	private void prepareGUI() {
		stage.setResizable(false);
		addIconToStage("/resources/program_icons/256x256.png");
		addIconToStage("/resources/program_icons/128x128.png");
		addIconToStage("/resources/program_icons/64x64.png");
		addIconToStage("/resources/program_icons/32x32.png");
		addIconToStage("/resources/program_icons/16x16.png");
		stage.setTitle("Kontrola | " + currentServer.getIP() + " [v" + PROGRAM_VERSION + "]");
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
	          public void handle(WindowEvent we) {
	              System.out.println("Stage is closing");
	              Inspection.endProgram();
	          }
	      });
		Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
		stage.setX((primScreenBounds.getWidth() - W_WIDTH) / 2);
		stage.setY((primScreenBounds.getHeight() - W_HEIGHT) / 2);
	}
	
	private void addIconToStage(String pathToIcon) {
		stage.getIcons().add(new Image(getInternalFile(pathToIcon).toString()));
	}
	
	private void showAgreementScreen() {
		checkMark.setX((W_WIDTH/2 - IMG_SIZE/2) - IMGS_OFFSET);
	    checkMark.setY(DEFAULT_IMG_Y);
	    checkMark.setFitHeight(IMG_SIZE);
	    checkMark.setFitWidth(IMG_SIZE);
	        
	    xMark.setX((W_WIDTH/2 - IMG_SIZE/2) + IMGS_OFFSET);
	    xMark.setY(DEFAULT_IMG_Y);
	    xMark.setFitHeight(IMG_SIZE);
	    xMark.setFitWidth(IMG_SIZE);
	       
	        
	    Text txtTitle = new Text(10, 35, "Souhlas�te s poskytnut�m serveru " + currentServer.getName() +
	    		" n�zev, typ nebo obsah n�kter�ch sv�ch soubor�?");
	    txtTitle.setFont(Font.font("Verdana", 19));
	    txtTitle.setTextAlignment(TextAlignment.CENTER);
	    txtTitle.setWrappingWidth(W_WIDTH - 10);
	        
		BorderPane pane = new BorderPane();
		pane.setBackground(getDefaultBackground());
		pane.getChildren().add(checkMark);
		pane.getChildren().add(xMark);
		pane.getChildren().add(txtTitle);
			
		Scene scene = new Scene(pane, W_WIDTH, W_HEIGHT);
		stage.setScene(scene);
		stage.show();
	        
		checkMark.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				pane.getChildren().clear();
				showInputNameScreen();
			}            
		});
		xMark.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
		    public void handle(MouseEvent event) {
				pane.getChildren().clear();
		        showDisagreedScreen();
			}            
		});
	}
	
	private void showInputNameScreen() {
		VBox vBox = new VBox();  
		vBox.setBackground(getDefaultBackground());
		vBox.setPadding(new Insets(10, 20, 150, 20));  
		vBox.setSpacing(10);

		Text txtTitle = new Text("Zadejte sv� hr��sk� jm�no");
		txtTitle.setFont(Font.font("Verdana", 19));
		txtTitle.setTextAlignment(TextAlignment.CENTER);
		txtTitle.setWrappingWidth(W_WIDTH - 35);
		txtTitle.setX(0);
		     
		TextArea nameArea = new TextArea(""); 
		nameArea.setStyle("-fx-font-size: 4em;");
		
		Button confirm = new Button("Potvrdit");
		confirm.setFont(Font.font("Verdana", 14));
		confirm.setMinWidth(100);
		confirm.setMinHeight(40);
		     
		Label errorArea = new Label();
		errorArea.setFont(Font.font("Verdana", FontWeight.BOLD, 15));
		errorArea.setTextFill(Color.web("#FF0000"));
		errorArea.setWrapText(true);
		     
		HBox hBox = new HBox();
		hBox.setSpacing(10);
		hBox.getChildren().addAll(confirm, errorArea);
		     
		vBox.getChildren().addAll(txtTitle, nameArea, hBox);
		     
		Scene scene = new Scene(vBox, W_WIDTH, W_HEIGHT);
		     
		stage.setScene(scene);
		stage.show();
		
		confirm.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
		    public void handle(MouseEvent event) {
				String error = handleBeginPress(nameArea.getText());
				nameArea.clear();
	    	    errorArea.setText(error);
			}            
		});
		     
		nameArea.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent key) {
				if (key.getCode().equals(KeyCode.ENTER)) {
					key.consume();
					String error = handleBeginPress(nameArea.getText());
		    	    nameArea.clear();
		    	    errorArea.setText(error);
				}
			}
		});
	}
	
	private Background getDefaultBackground() {
		BackgroundFill defaultBackgroundFill = new BackgroundFill(BACKGROUND_COLOR, new CornerRadii(1),
				new Insets(0.0,0.0,0.0,0.0));
		
		return new Background(defaultBackgroundFill);
	}
	
	public String handleBeginPress(String name) {
		boolean nameNotFoundInLogs = false;
		if (!isNameFormatCorrect(name)) {
			return "Form�t jm�na nen� spr�vn�.";
		}
		
		if (!inspection.isNameInLogs(name)) {
			nameNotFoundInLogs = true;
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Potenci�n� nespr�vn� jm�no!");
			alert.setHeaderText("Jm�no nebylo nalezeno. Opravdu se jedn� o va�e hr��sk� jm�no a chcete pokra�ovat?");
			alert.setContentText("P�esv�d�te se, �e jste va�e hr��sk� jm�no zadali spr�vn� a �e opravdu pat�� v�m!"
					+ " Pokud budete i p�esto pokra�ovat, bude informace o potenci�ln�"
					+ " nespr�vn�m/ciz�m jm�n� odesl�na a intern� pro�et�ena.");
			((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setText("I p�esto pokra�ovat");
			((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Zadat spr�vn� jm�no");
			alert.showAndWait();
			if (alert.getResult() == ButtonType.CANCEL) {
			    return "zadejte spr�vn� jm�no";
			}
		}
		startInspection(name, nameNotFoundInLogs);
		return "";
	}
	
	public boolean isNameFormatCorrect(String name) {
		return (name.length() >= MIN_NAME_LENGTH && name.length() <= MAX_NAME_LENGTH &&
				!UNALLOWED_NAME_CHARACTERS.matcher(name).find() &&
				Normalizer.isNormalized(name, Normalizer.Form.NFD));
	}
	
	private Requirement getUnfulfilledCondition() {
		try {
			final URL googleURL = new URL("https://www.google.com");
			final URLConnection connection1 = googleURL.openConnection();
			connection1.connect();
			connection1.setConnectTimeout(4000);
			connection1.setReadTimeout(4000);
			connection1.getInputStream().close();
			} catch (Exception e) {
				e.printStackTrace();
				return Requirement.INTERNET;
			}
		
		if (!Inspection.MINECRAFT_DIR.exists()) {
			return Requirement.MINECRAFT_DIR;
		}
		
		if (!Inspection.VERSIONS_DIR.exists()) {
			return Requirement.VERSIONS_DIR;
		}

		return null;
	}
	
	private void showUnfulfilledConditionScreen(Requirement podminka) {
	        Text txtTitle = new Text(10, 35, "Nelze pokra�ovat, proto�e");
	        txtTitle.setFont(Font.font("Verdana", 19));
	        txtTitle.setTextAlignment(TextAlignment.CENTER);
	        txtTitle.setWrappingWidth(W_WIDTH - 10);
	        
	        switch (podminka) {
	        	case INTERNET: {
	        		txtTitle.setText(txtTitle.getText() + " chyb� internetov� p�ipojen�.");
	        	} break;
	        	case MINECRAFT_DIR: {
	        		txtTitle.setText(txtTitle.getText() + " nebyl nalezen Minecraft.");
	        	} break;
	        	case VERSIONS_DIR: {
	        		txtTitle.setText(txtTitle.getText() + " nebyla nalezena slo�ka Versions.");
	        	} break;
	        }
	        txtTitle.setText(txtTitle.getText() + " Chcete to zkusit znovu?");
	        
	        exit.setX((W_WIDTH/2 - IMG_SIZE/2) + IMGS_OFFSET);
	        exit.setY(DEFAULT_IMG_Y);
	        exit.setFitHeight(IMG_SIZE);
	        exit.setFitWidth(IMG_SIZE);
	        
	        retry.setX((W_WIDTH/2 - IMG_SIZE/2) - IMGS_OFFSET);
	        retry.setY(DEFAULT_IMG_Y);
	        retry.setFitHeight(IMG_SIZE);
	        retry.setFitWidth(IMG_SIZE);
	        
	        BorderPane pane = new BorderPane();
	        pane.setBackground(getDefaultBackground());
	        pane.getChildren().addAll(retry, exit, txtTitle);
	        
	        Scene scene = new Scene(pane, W_WIDTH, W_HEIGHT);
	        stage.setScene(scene);
	        stage.show();
	        
	        retry.setOnMousePressed(new EventHandler<MouseEvent>() {
	            @Override
	            public void handle(MouseEvent event) {
	            	pane.getChildren().clear();
	            	checkConditionsAndStartProgram();
	            }            
	        });
	        
	        exit.setOnMousePressed(new EventHandler<MouseEvent>() {
	            @Override
	            public void handle(MouseEvent event) {
	            	Inspection.endProgram();
	            }            
	        });
 	}
	
	public void startInspection(String jmeno, boolean pravdepodobneNespravneJmeno) {
		inspectionRunning = true;
		inspection.supplyData(jmeno, pravdepodobneNespravneJmeno);
		synchronized (inspection)  {
			inspection.notify();
		}
		showInspectionRunningScreen("Prob�h� kontrola...");
		
		if (inspection.getPriority() != 9) {
			inspection.setPriority(9);
		}
		
		if (Thread.currentThread().getPriority() != 1) {
			Thread.currentThread().setPriority(1);	
		}
	}
	
	public void showInspectionCompletedScreen(ArrayList<String> chyby) {
	bringToFront();
	
	Text txtTitle = new Text(10, 35, chyby.size() > 0 ? "Kontrola byla dokon�ena s chybami. V�sledky byly odesl�ny."
			: "Kontrola byla dokon�ena bez chyb a v�sledky byly odesl�ny.");
	
     txtTitle.setFont(Font.font("Verdana", 19));
     txtTitle.setTextAlignment(TextAlignment.CENTER);
     txtTitle.setWrappingWidth(W_WIDTH - 10);
     
     exit.setX((W_WIDTH/2 - IMG_SIZE/2));
     exit.setY(W_HEIGHT - IMG_SIZE - 30);
     exit.setFitHeight(IMG_SIZE);
     exit.setFitWidth(IMG_SIZE);
	
	 BorderPane pane = new BorderPane();
	 pane.setBackground(getDefaultBackground());
     pane.getChildren().add(exit);
     pane.getChildren().add(txtTitle);
     
     Scene scene = new Scene(pane, W_WIDTH, W_HEIGHT);
     stage.setScene(scene);
     
     exit.setOnMousePressed(new EventHandler<MouseEvent>() {
         @Override
         public void handle(MouseEvent event) {
         	Inspection.endProgram();
         }
     });
	}
	
	public void bringToFront() {
		stage.setAlwaysOnTop(true);
		stage.requestFocus();
		stage.toFront();
		stage.setAlwaysOnTop(false);
	}
	
	private void showDisagreedScreen() {
		Text txtTitle = new Text(10, 35, "Dob�e, ��dn� data nebudou odesl�na.");
	        
	    txtTitle.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
	    txtTitle.setFont(Font.font("Verdana", 19));
	    txtTitle.setTextAlignment(TextAlignment.CENTER);
	    txtTitle.setWrappingWidth(W_WIDTH - 10);
	        
	    exit.setX((W_WIDTH/2 - IMG_SIZE/2) + IMGS_OFFSET);
	    exit.setY(DEFAULT_IMG_Y);
	    retry.setX((W_WIDTH/2 - IMG_SIZE/2) - IMGS_OFFSET);
	    retry.setY(DEFAULT_IMG_Y);
	        
	    exit.setFitHeight(IMG_SIZE);
	    exit.setFitWidth(IMG_SIZE);
	    retry.setFitHeight(IMG_SIZE);
	    retry.setFitWidth(IMG_SIZE);
	        
	    BorderPane pane = new BorderPane();
	    pane.setBackground(getDefaultBackground());
	    pane.getChildren().addAll(retry, exit, txtTitle);
	        
	    Scene scene = new Scene(pane, W_WIDTH, W_HEIGHT);
	        
	    stage.setScene(scene);
	        
	    retry.setOnMousePressed(new EventHandler<MouseEvent>() {
	    	@Override
	    	public void handle(MouseEvent event) {
	    		pane.getChildren().clear();
	    		showAgreementScreen();
	    	}            
	    });
	        
	    exit.setOnMousePressed(new EventHandler<MouseEvent>() {
	    	@Override
	    	public void handle(MouseEvent event) {
	    		Inspection.endProgram();
	    	}           
	    });
	}
	
	public static boolean isInspectionRunning() {
		return inspectionRunning;
	}
	
	public Server getCurrentServer() {
		return currentServer;
	}
}