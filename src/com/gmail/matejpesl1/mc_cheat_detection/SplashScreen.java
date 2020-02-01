package com.gmail.matejpesl1.mc_cheat_detection;

import java.awt.*;
import java.net.URL;
import javax.swing.*;

@SuppressWarnings("serial")
public class SplashScreen extends JWindow implements Runnable {
	private final Color borderColor;
    private final URL resourceURL;
    private final int duration;
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
         
        short width = 400;
        short height =130;
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width - width)/2;
        int y = (screen.height-height)/2;
        setBounds(x, y, width, height);
         
        JLabel logo = new JLabel(new ImageIcon(resourceURL));
        JLabel copyright =  new JLabel("© 2020 Matìj Pešl<matejpesl1@gmail.com>, All rights reserved.", JLabel.CENTER);
        
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
            
        } else {
        	synchronized (this) {
        		try {
        			wait();
        		} catch (InterruptedException e) {
        			System.out.println("interrupted - SplashScreen");
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
        return SplashScreen.class.getResource("/resources/logos/" + resourceName);
    }

	@Override
	public void run() {
		showSplash();	
	}
}