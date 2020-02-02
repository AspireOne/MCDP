package com.gmail.matejpesl1.mcdp.frontend;

import java.awt.*;
import java.net.URL;
import javax.swing.*;

@SuppressWarnings("serial")
public class SplashScreen extends JWindow implements Runnable {
	private final Color borderColor;
    private final URL resourceURL;
    private final int duration;
    private final static short HEIGHT =260;
    private final static short WIDTH = 520;
    Main owner;
    
    public SplashScreen(Color borderColor, String resourceName, int duration, Main owner) {
    	this.duration = duration;
    	this.borderColor = borderColor;
    	resourceURL = getLogo(resourceName);
    	this.owner = owner;
    }

    public void showSplash() {
        JPanel panel = (JPanel)getContentPane();
        panel.setBackground(Color.white);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width - WIDTH)/2;
        int y = (screen.height-HEIGHT)/2;
        setBounds(x, y, WIDTH, HEIGHT);
         
        JLabel logo = new JLabel(new ImageIcon(resourceURL));
        JLabel copyright =  new JLabel("© 2020 Matìj Pešl<matejpesl1@gmail.com>, All rights reserved.", JLabel.CENTER);
        
        copyright.setFont(new Font("Sans-Serif", Font.ITALIC, 13));
        panel.add(copyright, BorderLayout.SOUTH);
        panel.add(logo);
        panel.setBorder(BorderFactory.createLineBorder(borderColor, 5));

        setVisible(true);
        
        if (duration != 0) {
            try {
            	Thread.sleep(duration);
            } catch (Exception e) {
            	e.printStackTrace();
            } finally {
                close();
            }
            
        } else {
        	synchronized (this) {
        		try {
        			while (owner.splashShouldBeShown) {
        				wait();	
        			}
        		} catch (InterruptedException e) {
        			System.out.println("SplashScreen interrupted succesfully");
        		} finally {
        			close();
        		}
        	}	
        }
    }
    
    public void close() {
    	setVisible(false);
    	dispose();
    }
    
    public static URL getLogo(String resourceName) {
        return SplashScreen.class.getResource("/com/gmail/matejpesl1/mcdp/resources/logos/" + resourceName);
    }

	@Override
	public void run() {
		showSplash();	
	}
}