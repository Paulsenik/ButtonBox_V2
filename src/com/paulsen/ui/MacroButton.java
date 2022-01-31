package com.paulsen.ui;

import java.awt.Component;
import java.awt.Graphics;

public class MacroButton extends PUIText {

	private PUIText normalKey, functionKey, customKey;

	public MacroButton(Component l, PUIAction normalKeyAction, PUIAction functionKeyAction, PUIAction customKeyAction) {
		super(l);
		normalKey = new PUIText(l, "N");
		functionKey = new PUIText(l, "F");
		customKey = new PUIText(l, "C");
		normalKey.addActionListener(normalKeyAction);
		functionKey.addActionListener(functionKeyAction);
		customKey.addActionListener(customKeyAction);
	}

	@Override
	public void setBounds(int x, int y, int w, int h) {
		super.setBounds(x, y, w - h, h);
		normalKey.setBounds(x + w - h, y, h , h/ 3);
		functionKey.setBounds(x + w - h, y+(h/3), h , h/ 3);
		customKey.setBounds(x + w - h, y+(h/3*2), h , h/ 3);
	}
	
	@Override
	public void draw(Graphics g) {
		super.draw(g);
		normalKey.draw(g);
		functionKey.draw(g);
		customKey.draw(g);
	}
	
	@Override
	public void setMetadata(Object o) {
		super.setMetadata(o);
		normalKey.setMetadata(o);
		functionKey.setMetadata(o);
		customKey.setMetadata(o);
	}

}
