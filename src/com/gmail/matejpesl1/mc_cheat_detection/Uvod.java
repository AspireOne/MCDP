package com.gmail.matejpesl1.mc_cheat_detection;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.GroupLayout.Alignment;

import com.gmail.matejpesl1.mc_cheat_detection.Kontrola;
import com.gmail.matejpesl1.servers.Basicland;
import com.gmail.matejpesl1.servers.Debug;
import com.gmail.matejpesl1.servers.Server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;


public class Uvod extends Application {
	private static final int W_WIDTH = 500;
	private static final int W_HEIGHT = 200;
	private static final int IMG_SIZE = 70;
	private static final int DEFAULT_IMG_Y = (W_HEIGHT/2 - IMG_SIZE/2) + 50;
	private static final int IMGS_OFFSET = 100;
	
	public static final Rezim rezim = Rezim.DEBUG;
	public static final Pattern NEPOVOLENE_ZNAKY_VE_JMENE = Pattern.compile("[$&+,:;=\\\\?@#|/'<>.^*() %!-]");
	public static final float VERZE_PROGRAMU = 2.6f;
	public static final int MAX_DELKA_JMENA = 16;
	public static final int MIN_DELKA_JMENA = 3;
	private static boolean inspectionRunning = false;
	public static enum Podminka{PRIPOJENI_K_INTERNETU, SLOZKA_MINECRAFT};
	public static enum Rezim {DEBUG, BASICLAND};
	private Kontrola kontrola;
	private Server currentServer;
	private Stage stage;
	
	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage stage) {
		this.stage = stage;
		kontrola = new Kontrola(this);
		kontrola.start();
		currentServer = determineServer(rezim);
		showSplashScreen(3000);
		
		prepareGUI();
		
		showUpdateScreen();
		
		showAgreementScreen();
	
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
	
	public void handleNameInput() {
		
	}
	
	public void showInspectionRunningScreen(String state) {
		try {	
        Text txt = new Text(10, 35, state);
       
        txt.setFont(Font.font("Verdana", 19));
        txt.setTextAlignment(TextAlignment.CENTER);
        txt.setWrappingWidth(W_WIDTH - 10);
        
        BorderPane pane = new BorderPane();
        pane.setBackground(getDefaultBackground());
        pane.getChildren().add(txt);

        Scene scene = new Scene(pane, W_WIDTH, W_HEIGHT);
        stage.setScene(scene);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void changeInspectionState(String state) {
		showInspectionRunningScreen(state);
	}
	
	private void showUpdateScreen() {
        Text progress = new Text(10, 35, "Probíhá kontrola aktualizací...");
        progress.setFont(Font.font("Verdana", 19));
        progress.setTextAlignment(TextAlignment.CENTER);
        progress.setWrappingWidth(W_WIDTH - 10);
        
		BorderPane pane = new BorderPane();
		pane.setBackground(getDefaultBackground());
		pane.getChildren().add(progress);

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
			progress.setText("Stahování...");
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
		alert.setHeaderText(chyba);	
		alert.showAndWait();
	}
	
	private void prepareGUI() {
		stage.setResizable(false);
		stage.setTitle("Cheat Detector");
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
	          public void handle(WindowEvent we) {
	              System.out.println("Stage is closing");
	              kontrola.ukonci();
	          }
	      });
		stage.centerOnScreen();
	}
	
	private void showAgreementScreen() {
			FileInputStream yInput = null;
			FileInputStream nInput = null;
			try {
				yInput = new FileInputStream("C:\\Users\\42073\\Desktop\\y2.png");
				nInput = new FileInputStream("C:\\Users\\42073\\Desktop\\n2.png");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
	        ImageView n = new ImageView(new Image(nInput));
	        ImageView y = new ImageView(new Image(yInput));
	        
	        y.setX((W_WIDTH/2 - IMG_SIZE/2) - IMGS_OFFSET);
	        y.setY(DEFAULT_IMG_Y);
	        n.setX((W_WIDTH/2 - IMG_SIZE/2) + IMGS_OFFSET);
	        n.setY(DEFAULT_IMG_Y);
	        
	        n.setFitHeight(IMG_SIZE);
	        n.setFitWidth(IMG_SIZE);
	        y.setFitHeight(IMG_SIZE);
	        y.setFitWidth(IMG_SIZE);
	        
	        Text txt = new Text(10, 35, "Souhlasíte s poskytnutím serveru " + currentServer.getName() +
	        		" název, typ nebo obsah nìkterých svých souborù?");
	        txt.setFont(Font.font("Verdana", 19));
	        txt.setTextAlignment(TextAlignment.CENTER);
	        txt.setWrappingWidth(W_WIDTH - 10);
	        
			BorderPane pane = new BorderPane();
			pane.setBackground(getDefaultBackground());
			pane.getChildren().add(y);
			pane.getChildren().add(n);
			pane.getChildren().add(txt);
			
			Scene scene = new Scene(pane, W_WIDTH, W_HEIGHT);
			stage.setScene(scene);

			stage.show();
	        
			 y.setOnMousePressed(new EventHandler<MouseEvent>() {
		            @Override
		            public void handle(MouseEvent event) {
		            	System.out.println("souhlas");
		            	pane.getChildren().clear();
		            	showInputNameScreen();
		            }            
		        });
			 n.setOnMousePressed(new EventHandler<MouseEvent>() {
		            @Override
		            public void handle(MouseEvent event) {
		            	System.out.println("nesouhlas");
		            	pane.getChildren().clear();
		            	showDisagreedScreen();
		            }            
		        });
	}
	
	private void showInputNameScreen() {
		     VBox box = new VBox();  
		     box.setBackground(getDefaultBackground());
		     box.setPadding(new Insets(10, 20, 150, 20));  
		     box.setSpacing(10);

		     Text title = new Text("Zadejte své hráèské jméno");
		     title.setFont(Font.font("Verdana", 19));
		     title.setTextAlignment(TextAlignment.CENTER);
		     title.setWrappingWidth(W_WIDTH - 35);
		     title.setX(0);
		     
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
		     
		     box.getChildren().addAll(title, nameArea);
		     HBox hBox = new HBox();
		     hBox.setSpacing(10);
		     hBox.getChildren().addAll(confirm, errorArea);
		     box.getChildren().add(hBox);
		     
		     Scene scene = new Scene(box, W_WIDTH, W_HEIGHT);
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
		    	            System.out.println("potvrdit");
		    	            key.consume();
		    	            String error = handleBeginPress(nameArea.getText());
		    	            nameArea.clear();
		    	            errorArea.setText(error);
		    	        }
		    	    }
		    });
	}
	
	private Background getDefaultBackground() {
		BackgroundFill myBF = new BackgroundFill(Color.BLUEVIOLET, new CornerRadii(1),
				new Insets(0.0,0.0,0.0,0.0));// or null for the padding
		//then you set to your node or container or layout
		return new Background(myBF);
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
					+ "Pokud budete i pøesto pokraèovat, bude informace o potenciálnì"
					+ " nesprávném/cizím jménì odeslána a internì prošetøena.");
			((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setText("I pøesto pokraèovat");
			((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Zadat správné jméno");
			alert.showAndWait();
			if (alert.getResult() == ButtonType.CANCEL) {
			    return "zadejte správné jméno";
			}
			System.out.println("ssdasdsad");
		}

		Podminka unfulfilledCondition = getUnfulfilledCondition();	
		if (unfulfilledCondition != null) {
			switch (getUnfulfilledCondition()) {
				case SLOZKA_MINECRAFT: {
					showUnfulfilledConditionScreen(Podminka.SLOZKA_MINECRAFT);
				} break;
				case PRIPOJENI_K_INTERNETU: {
					showUnfulfilledConditionScreen(Podminka.PRIPOJENI_K_INTERNETU);
				} break;
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
		
		return null;
	}
	
	private void showUnfulfilledConditionScreen(Podminka podminka) {
		try {
			FileInputStream eInput = new FileInputStream("C:\\Users\\42073\\Desktop\\e2.png");
			FileInputStream zInput = new FileInputStream("C:\\Users\\42073\\Desktop\\z2.png");
			ImageView e = new ImageView(new Image(eInput));
	        ImageView z = new ImageView(new Image(zInput));
	        
	        Text txt1 = new Text(10, 35, "Odejít");
	        Text txt2 = new Text(10, 35, "Zkusit znovu");
	        Text txt = new Text(10, 35, "Kontrola nemohla být spuštìna, protože");
	        
	        switch (podminka) {
	        case PRIPOJENI_K_INTERNETU: {
	        	txt.setText(txt.getText() + " chybý internetové pøipojení.");
	        } break;
	        case SLOZKA_MINECRAFT: {
	        	txt.setText(txt.getText() + " nebyl nalezen Minecraft.");
	        } break;
	        }
	        txt.setText(txt.getText() + " Chcete to zkusit znovu?");
	        txt1.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
	        txt2.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
	        txt.setFont(Font.font("Verdana", 19));
	        txt.setTextAlignment(TextAlignment.CENTER);
	        txt.setWrappingWidth(W_WIDTH - 10);
	        
	        e.setX((W_WIDTH/2 - IMG_SIZE/2) + IMGS_OFFSET);
	        e.setY(DEFAULT_IMG_Y);
	        z.setX((W_WIDTH/2 - IMG_SIZE/2) - IMGS_OFFSET);
	        z.setY(DEFAULT_IMG_Y);
	        
	        e.setFitHeight(IMG_SIZE);
	        e.setFitWidth(IMG_SIZE);
	        z.setFitHeight(IMG_SIZE);
	        z.setFitWidth(IMG_SIZE);
	        
	        txt1.setX(e.getX() + 10);
	        txt1.setY(e.getY() - 12);
	        txt2.setX(z.getX() - 20);
	        txt2.setY(z.getY() - 12);
	        
	        BorderPane pane = new BorderPane();
	        pane.setBackground(getDefaultBackground());
	        pane.getChildren().add(z);
	        pane.getChildren().add(e);
	        pane.getChildren().add(txt);
	        pane.getChildren().add(txt1);
	        pane.getChildren().add(txt2);
	        
	        Scene scene = new Scene(pane, W_WIDTH, W_HEIGHT);
	        stage.setScene(scene);
	        
	        z.setOnMousePressed(new EventHandler<MouseEvent>() {
	            @Override
	            public void handle(MouseEvent event) {
	            	System.out.println("retry");
	            	pane.getChildren().clear();
	            	showInputNameScreen();
	            }            
	        });
	        
	        e.setOnMousePressed(new EventHandler<MouseEvent>() {
	            @Override
	            public void handle(MouseEvent event) {
	            	System.out.println("exit");
	            	Platform.exit();
	            }            
	        });
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	try {
		
	FileInputStream eInput = new FileInputStream("C:\\Users\\42073\\Desktop\\e2.png");
	ImageView e = new ImageView(new Image(eInput));
	
	Text txt = new Text(10, 35, chyby.size() > 0 ? "Kontrola byla dokonèena s chybama. Výsledky byly odeslány."
			: "Kontrola byla dokonèena bez chyb a výsledky byly odeslány.");
	
     txt.setFont(Font.font("Verdana", 19));
     txt.setTextAlignment(TextAlignment.CENTER);
     txt.setWrappingWidth(W_WIDTH - 10);
     
     e.setX((W_WIDTH/2 - IMG_SIZE/2));
     e.setY(W_HEIGHT - IMG_SIZE - 30);
     
     e.setFitHeight(IMG_SIZE);
     e.setFitWidth(IMG_SIZE);
	
	 BorderPane pane = new BorderPane();
	 pane.setBackground(getDefaultBackground());
     pane.getChildren().add(e);
     pane.getChildren().add(txt);
     
     Scene scene = new Scene(pane, W_WIDTH, W_HEIGHT);
     stage.setScene(scene);
     
     e.setOnMousePressed(new EventHandler<MouseEvent>() {
         @Override
         public void handle(MouseEvent event) {
         	kontrola.ukonci();
         }
     });
	} catch (Exception e) {
		e.printStackTrace();
		}
	}
	
	public void bringToFront() {
		stage.setAlwaysOnTop(true);
		stage.requestFocus();
		stage.toFront();
		stage.setAlwaysOnTop(false);
	}
	
	private void showDisagreedScreen() {
		try {
			FileInputStream eInput = new FileInputStream("C:\\Users\\42073\\Desktop\\e2.png");
			FileInputStream zInput = new FileInputStream("C:\\Users\\42073\\Desktop\\z2.png");
			ImageView e = new ImageView(new Image(eInput));
	        ImageView z = new ImageView(new Image(zInput));
	        
	        Text txt1 = new Text(10, 35, "Odejít");
	        Text txt2 = new Text(10, 35, "Zmìnit odpovìï");
	        Text txt = new Text(10, 35, "Dobøe, žádná data nebudou odeslána.");
	        
	        txt1.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
	        txt2.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
	        txt.setFont(Font.font("Verdana", 19));
	        txt.setTextAlignment(TextAlignment.CENTER);
	        txt.setWrappingWidth(W_WIDTH - 10);
	        
	        e.setX((W_WIDTH/2 - IMG_SIZE/2) + IMGS_OFFSET);
	        e.setY(DEFAULT_IMG_Y);
	        z.setX((W_WIDTH/2 - IMG_SIZE/2) - IMGS_OFFSET);
	        z.setY(DEFAULT_IMG_Y);
	        
	        e.setFitHeight(IMG_SIZE);
	        e.setFitWidth(IMG_SIZE);
	        z.setFitHeight(IMG_SIZE);
	        z.setFitWidth(IMG_SIZE);
	        
	        txt1.setX(e.getX() + 10);
	        txt1.setY(e.getY() - 12);
	        txt2.setX(z.getX() - 20);
	        txt2.setY(z.getY() - 12);
	        
	        BorderPane pane = new BorderPane();
	        pane.setBackground(getDefaultBackground());
	        pane.getChildren().add(z);
	        pane.getChildren().add(e);
	        pane.getChildren().add(txt);
	        pane.getChildren().add(txt1);
	        pane.getChildren().add(txt2);
	        
	        Scene scene = new Scene(pane, W_WIDTH, W_HEIGHT);
	        stage.setScene(scene);
	        
	        z.setOnMousePressed(new EventHandler<MouseEvent>() {
	            @Override
	            public void handle(MouseEvent event) {
	            	System.out.println("retry");
	            	pane.getChildren().clear();
	            	showAgreementScreen();
	            }            
	        });
	        
	        e.setOnMousePressed(new EventHandler<MouseEvent>() {
	            @Override
	            public void handle(MouseEvent event) {
	            	System.out.println("exit");
	            	Platform.exit();
	            }            
	        });
	        
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static boolean isInspectionRunning() {
		return inspectionRunning;
	}
	
	public Server getCurrentServer() {
		return currentServer;
	}
}