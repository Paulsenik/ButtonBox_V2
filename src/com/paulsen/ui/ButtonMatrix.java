package com.paulsen.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import com.paulsen.ButtonBox.Button;
import com.paulsen.Main;
import com.paulsen.ui.PUIElement;
import com.paulsen.ui.PUIPaintable;

public class ButtonMatrix extends PUIElement {

	public PUIElement buttons[][] = new PUIElement[4][4];
	public int selectedIndex = -1;

	public Point lastMouse;

	public ButtonMatrix(Component l) {
		super(l);
		l.addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent e) {
				lastMouse = e.getPoint();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
			}
		});

		for (int i = 0; i < buttons.length; i++) {
			for (int j = 0; j < buttons[i].length; j++) {
				PUIElement b = new PUIElement(c);
				b.setDraw(new PUIPaintable() {
					@Override
					public void paint(Graphics g, int x, int y, int w, int h) {
						g.fillOval(x + 2, y + 2, w - 4, h - 4);
						g.setColor(Color.black);
						g.drawOval(x + 2, y + 2, w - 4, h - 4);
						g.drawOval(x + 3, y + 3, w - 6, h - 6);
					}
				});
				buttons[i][j] = b;
			}
		}
	}

	public void setSelected() {
		for (int i = 0; i < buttons.length; i++)
			for (int j = 0; j < buttons.length; j++)
				if (buttons[i][j].getBounds().intersects(new Rectangle(lastMouse.x, lastMouse.y, 1, 1))) {
					if (selectedIndex != i * 4 + j)
						selectedIndex = i * 4 + j;
					else
						selectedIndex = -1;
					return;
				}
	}

	@Override
	public void setBounds(int x, int y, int w, int h) {
		super.setBounds(x, y, w, h);
		for (int i = 0; i < buttons.length; i++) {
			for (int j = 0; j < buttons.length; j++) {
				buttons[i][j].setBounds(x + (w / 4) * j, y + (h / 4) * i, w / 4, h / 4);
			}
		}
	}

	@Override
	public void draw(Graphics g) {
		for (int i = 0; i < buttons.length; i++)
			for (int j = 0; j < buttons[i].length; j++) {
				if (Main.bb == null) {
					g.setColor(Color.red);
				} else {
					Button b = Main.bb.getButton(Main.currentPage * 16 + i * 4 + j);
					if (Main.currentPage * 16 + i * 4 + j == Main.selectedButtonIndex
							&& Main.selectedButtonIndex != -1) { // selected

						g.setColor(Color.green); // sel
					} else { // not selected
						if (b != null && !b.isEmpty()) {
							g.setColor(Color.yellow); // full
						} else {
							g.setColor(new Color(255, 111, 0));// empty
						}
					}
				}
				buttons[i][j].draw(g);
			}
	}
}
