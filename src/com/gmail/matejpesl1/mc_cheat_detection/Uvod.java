package com.gmail.matejpesl1.mc_cheat_detection;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.regex.Pattern;

import com.gmail.matejpesl1.mc_cheat_detection.Kontrola;
import com.gmail.matejpesl1.servers.Basicland;
import com.gmail.matejpesl1.servers.Debug;
import com.gmail.matejpesl1.servers.Server;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
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


public class Uvod extends Application {
	public static enum Rezim {DEBUG, BASICLAND};
	public static final Rezim rezim = Rezim.DEBUG;
	public static final float VERZE_PROGRAMU = 2.6f;
	
	private static final int SPLASH_DURATION = 3000;
	
	private static final int W_WIDTH = 500;
	private static final int W_HEIGHT = 200;
	private static final int IMG_SIZE = 70;
	private static final int DEFAULT_IMG_Y = (W_HEIGHT/2 - IMG_SIZE/2) + 50;
	private static final int IMGS_OFFSET = 100;
	private static final int LOGO_SIZE = IMG_SIZE + 50;
	
	public static ImageView checkMark;
	public static ImageView xMark;
	public static ImageView exit;
	public static ImageView programLogo;
	public static ImageView retry;
	
	public static final Pattern NEPOVOLENE_ZNAKY_VE_JMENE = Pattern.compile("[$&+,:;=\\\\?@#|/'<>.^*() %!-]");
	public static final int MAX_DELKA_JMENA = 16;
	public static final int MIN_DELKA_JMENA = 3;
	
	private static boolean inspectionRunning = false;
	public static enum Podminka{PRIPOJENI_K_INTERNETU, SLOZKA_MINECRAFT, SLOZKA_VERSIONS};
	private Kontrola kontrola;
	private Server currentServer;
	private Stage stage;
	
	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage stage) {
		this.stage = stage;
		loadImages();
		prepareGUI();
		begin();
	}
	public void begin() {
		Podminka unfulfilledCondition = getUnfulfilledCondition();
		if (unfulfilledCondition != null) {
			showUnfulfilledConditionScreen(unfulfilledCondition);
		} else {
			kontrola = new Kontrola(this);
			kontrola.start();

			currentServer = determineServer(rezim);
			if (rezim != Rezim.DEBUG) {
				showSplashScreen(SPLASH_DURATION);	
			} else {
				showSplashScreen(500);
			}
			
			showUpdateScreen();
			
			showAgreementScreen();	
		}
	}
	
	private void loadImages() {
		programLogo = new ImageView(new Image(getInternalFile("/resources/program_icons/256x256.png").toString()));
		checkMark = new ImageView(new Image(getInternalFile("/resources/UI/checkmark.png").toString()));	
		xMark = new ImageView(new Image(getInternalFile("/resources/UI/xmark.png").toString()));
		exit = new ImageView(new Image(getInternalFile("/resources/UI/exit.png").toString()));
		retry = new ImageView(new Image(getInternalFile("/resources/UI/retry.png").toString()));
	}

	 public static URL getInternalFile(String path) {
		 return Uvod.class.getResource(path);
	 }
	
	private void showSplashScreen(int duration) {
		try {
			SplashScreen splash =
					new SplashScreen(currentServer.getBorderColor(), currentServer.getLogo(), duration);
			splash.run();
		} catch(Exception e) {
			e.printStackTrace();
			kontrola.chyby.add("Nepodaøilo se spustit uvítací obrazovku (splash screen).");
		}
	}
	
	private Server determineServer(Rezim rezim) {
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
        Text txtProgress = new Text(10, 35, "Probíhá kontrola aktualizací...");
        txtProgress.setFont(Font.font("Verdana", 19));
        txtProgress.setTextAlignment(TextAlignment.CENTER);
        txtProgress.setWrappingWidth(W_WIDTH - 10);
        
		BorderPane pane = new BorderPane();
		pane.setBackground(getDefaultBackground());
		pane.getChildren().add(txtProgress);

		//this is here just because of bug - this is a workaround
		stage.setScene(new Scene(new BorderPane(), W_WIDTH, W_HEIGHT));
		stage.show();
		//the workaround ends here
		
		Scene scene = new Scene(pane, W_WIDTH, W_HEIGHT);
		stage.setScene(scene);
		stage.show();
		
		Update update = null;
		try {			
			update = new Update(this);
		} catch (URISyntaxException e) {
			showErrorInUpdateProcess("Program nedokázal najít cestu sám k sobì.", e);
			showAgreementScreen();
		} catch (IOException e) {
			showErrorInUpdateProcess("Nepodaøila se ovìøit dostupnost novìjší verze.", e);
			showAgreementScreen();
		}
		    
		if (update.isUpdateAvailable()) {	
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Aktualizace");
			alert.setHeaderText("Aktualizace dostupná! Bude stažena a program bude restartován.");
			txtProgress.setText("Stahování...");
			alert.showAndWait();
			update(update);
		}
		
	}
	
	private void update(Update aktualizace) {
		try {
			aktualizace.downloadUpdate();	
		} catch (IOException e) {
			showErrorInUpdateProcess("Nepodaøilo se stáhnout novou verzi, pokraèuji se starou...", e);
			showAgreementScreen();
		}
		
		try {
			aktualizace.update();	
		} catch (IOException e) {
			showErrorInUpdateProcess("Aktualizace se nezdaøila, pokraèuji se starou verzí...", e);
			showAgreementScreen();
		}
	}
	
	private void showErrorInUpdateProcess(String error, Exception e) {
		e.printStackTrace();
		kontrola.chyby.add(error);
		showErrorAlert(error);
		showAgreementScreen();
	}
	
	private void showErrorAlert(String chyba) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Chyba");
		alert.setHeaderText("Chyba: " + chyba);
		alert.showAndWait();
	}
	
	private void prepareGUI() {
		stage.setResizable(false);
		stage.setTitle("Cheat Detector");
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
	          public void handle(WindowEvent we) {
	              System.out.println("Stage is closing");
	              Kontrola.ukonci();
	          }
	      });
		stage.centerOnScreen();
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
	       
	        
	    Text txtTitle = new Text(10, 35, "Souhlasíte s poskytnutím serveru " + currentServer.getName() +
	    		" název, typ nebo obsah nìkterých svých souborù?");
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

		Text txtTitle = new Text("Zadejte své hráèské jméno");
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
		BackgroundFill defaultBackgroundFill = new BackgroundFill(Color.BLUEVIOLET, new CornerRadii(1),
				new Insets(0.0,0.0,0.0,0.0));
		
		return new Background(defaultBackgroundFill);
	}
	
	public String handleBeginPress(String name) {
		boolean nameFoundInLogs = true;
		if (!isNameFormatCorrect(name)) {
			return "Formát jména není správný.";
		}
		
		if (!isNameInLogs(name)) {
			nameFoundInLogs = false;
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Potenciánì nesprávné jméno!");
			alert.setHeaderText("Jméno nebylo nalezeno. Opravdu se jedná o vaše hráèské jméno a chcete pokraèovat?");
			alert.setContentText("Pøesvìdète se, že jste vaše hráèské jméno zadali správnì a že opravdu patøí vám!"
					+ " Pokud budete i pøesto pokraèovat, bude informace o potenciálnì"
					+ " nesprávném/cizím jménì odeslána a internì prošetøena.");
			((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setText("I pøesto pokraèovat");
			((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Zadat správné jméno");
			alert.showAndWait();
			if (alert.getResult() == ButtonType.CANCEL) {
			    return "zadejte správné jméno";
			}
		}
		startInspection(name, nameFoundInLogs);
		return "";
	}
	
	public boolean isNameInLogs(String name) {
		return new Kontrola(null).jeJmenoNalezeno(name);
	}
	
	public boolean isNameFormatCorrect(String name) {
		return (name.length() >= MIN_DELKA_JMENA && name.length() <= MAX_DELKA_JMENA &&
				!NEPOVOLENE_ZNAKY_VE_JMENE.matcher(name).find() &&
				Normalizer.isNormalized(name, Normalizer.Form.NFD));
	}
	
	private Podminka getUnfulfilledCondition() {
		try {
			final URL googleURL = new URL("https://www.google.com");
			final URLConnection connection1 = googleURL.openConnection();
			connection1.connect();
			connection1.setConnectTimeout(6000);
			connection1.setReadTimeout(4000);
			connection1.getInputStream().close();
			} catch (Exception e) {
				e.printStackTrace();
				return Podminka.PRIPOJENI_K_INTERNETU;
				}
		
		if (!Kontrola.SLOZKA_MINECRAFT.exists()) {
			return Podminka.SLOZKA_MINECRAFT;
		}
		
		if (!Kontrola.SLOZKA_VERSIONS.exists()) {
			return Podminka.SLOZKA_VERSIONS;
		}
		
		return null;
	}
	
	private void showUnfulfilledConditionScreen(Podminka podminka) {
	        Text txtExit = new Text(10, 35, "Odejít");
	        txtExit.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
	        
	        Text txtRetry = new Text(10, 35, "Zkusit znovu");
	        txtRetry.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
	        
	        Text txtTitle = new Text(10, 35, "Kontrola nemohla být spuštìna, protože");
	        txtTitle.setFont(Font.font("Verdana", 19));
	        txtTitle.setTextAlignment(TextAlignment.CENTER);
	        txtTitle.setWrappingWidth(W_WIDTH - 10);
	        
	        switch (podminka) {
	        	case PRIPOJENI_K_INTERNETU: {
	        		txtTitle.setText(txtTitle.getText() + " chybý internetové pøipojení.");
	        	} break;
	        	case SLOZKA_MINECRAFT: {
	        		txtTitle.setText(txtTitle.getText() + " nebyl nalezen Minecraft.");
	        	} break;
	        	case SLOZKA_VERSIONS: {
	        		txtTitle.setText(txtTitle.getText() + " nebyla nalezena složka Versions.");
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
	        
	        txtExit.setX(exit.getX() + 10);
	        txtExit.setY(exit.getY() - 12);
	        txtRetry.setX(retry.getX() - 20);
	        txtRetry.setY(retry.getY() - 12);
	        
	        BorderPane pane = new BorderPane();
	        pane.setBackground(getDefaultBackground());
	        pane.getChildren().addAll(retry, exit, txtTitle, txtExit, txtRetry);
	        
	        Scene scene = new Scene(pane, W_WIDTH, W_HEIGHT);
	        stage.setScene(scene);
	        stage.show();
	        
	        retry.setOnMousePressed(new EventHandler<MouseEvent>() {
	            @Override
	            public void handle(MouseEvent event) {
	            	pane.getChildren().clear();
	            	begin();
	            }            
	        });
	        
	        exit.setOnMousePressed(new EventHandler<MouseEvent>() {
	            @Override
	            public void handle(MouseEvent event) {
	            	Kontrola.ukonci();
	            }            
	        });
 	}
	
	public void startInspection(String jmeno, boolean pravdepodobneNespravneJmeno) {
		inspectionRunning = true;
		kontrola.dodejUdaje(jmeno, pravdepodobneNespravneJmeno);
		synchronized (kontrola)  {
			kontrola.notify();   
		}
		showInspectionRunningScreen("Probíhá kontrola...");
	}
	
	public void showInspectionCompletedScreen(ArrayList<String> chyby) {
	bringToFront();
	
	Text txtTitle = new Text(10, 35, chyby.size() > 0 ? "Kontrola byla dokonèena s chybami. Výsledky byly odeslány."
			: "Kontrola byla dokonèena bez chyb a výsledky byly odeslány.");
	
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
         	Kontrola.ukonci();
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
		Text txtTitle = new Text(10, 35, "Dobøe, žádná data nebudou odeslána.");
	        
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
	    		Kontrola.ukonci();
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