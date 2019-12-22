package com.gmail.matejpesl1.mc_cheat_detection;

import java.awt.*;
import java.net.URL;
import javax.swing.*;

@SuppressWarnings("serial")
public class SplashScreen extends JWindow implements Runnable {
	private final Color borderColor;
    private final URL resourceURL;
    private final int duration;
     
    public SplashScreen(Color borderColor, String resourceName, int duration) {
    	this.duration = duration;
    	this.borderColor = borderColor;
    	resourceURL = getInternalFile(resourceName);
    }

    public void showSplash() {  
        JPanel panel = (JPanel)getContentPane();
        panel.setBackground(Color.white);
         
        int width = 400;
        int height =130;
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width - width)/2;
        int y = (screen.height-height)/2;
        setBounds(x, y, width, height);
         
        JLabel logo = new JLabel(new ImageIcon(resourceURL));
        JLabel copyright =  new JLabel("© 2019 Matìj Pešl, All rights reserved.", JLabel.CENTER);
        
        copyright.setFont(new Font("Sans-Serif", Font.ITALIC, 11));
        panel.add(copyright, BorderLayout.SOUTH);
        panel.add(logo);
        panel.setBorder(BorderFactory.createLineBorder(borderColor, 3));

        setVisible(true);
        
        if (duration != 0) {
            try {
            	Thread.sleep(duration);
            } catch (Exception e) {
            	e.printStackTrace();
            } finally {
                close();	
            }
        }
    }
    
    public void close() {
    	setVisible(false);
    	dispose();
    }
    
    public static URL getInternalFile(String resourceName) {
        return SplashScreen.class.getResource("/resources/" + resourceName);
    }

	@Override
	public void run() {
		showSplash();
	}
}