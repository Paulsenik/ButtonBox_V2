package com.paulsen.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;

import com.paulsen.ButtonBox.Button;
import com.paulsen.Main;
import com.paulsen.SerialManager;
import com.paulsen.io.PDataStorage;
import com.paulsen.io.PFile;
import com.paulsen.io.PFolder;
import com.paulsen.ui.PUIElement;
import com.paulsen.ui.PUIFrame;
import com.paulsen.ui.PUIInitializable;
import com.paulsen.ui.PUIPaintable;
import com.paulsen.ui.PUIText;
import com.paulsen.ui.PUIUpdatable;

public class UI {
	private static final long serialVersionUID = 1L;

	public PUIFrame f;

	// Elements
	private PUIText connectB, downloadB, uploadB, currentPageB;
	private PUIElement prevPage, nextPage;
	private ButtonMatrix buttonMatrix;

	private PUIText boxInfos[] = new PUIText[5]; // 0=version, 1=size, 2=pages, 3=buttons, 4=usedBytes

	private PUIText keyInfo;
	private MacroButton macros[] = new MacroButton[Main.MAXMACROSIZE];
	private PUIText resetMacro;

	// Variables
	int leftMenuBarWidth, topMenuBarHeight, defaultButtonSize, space, buttonMatrixWidth, pageSettingsWidth;

	public UI() {
		PUIElement.darkUIMode = true;
		f = new PUIFrame("Button Box (V2) - Programmer", 1400, 800, new PUIInitializable() {
			@Override
			public void initUI(Component c) {
				connectB = new PUIText(c, "-");
				connectB.addActionListener(new PUIAction() {
					@Override
					public void run(PUIElement that) {
						if (!Main.sm.isConnected()) {
							int chosenPort = f.getUserSelection("Connect to Arduino:", SerialManager.getAllPorts());
							Main.initSerial(SerialManager.getAllPorts()[chosenPort]);
						} else {
							Main.sm.close();
							Main.bb = null;
						}
						f.updateElements();
					}
				});
				downloadB = new PUIText(c);
				downloadB.addActionListener(new PUIAction() {
					@Override
					public void run(PUIElement that) {
						if (Main.isDownloading) {
							return;
						}
						if (Main.sm != null && Main.sm.isConnected()) {
							if (Main.bb != null && Main.bb.getSize() != 0)
								Main.download();
							else {
								Main.getBoardInfo();
							}
						}
					}
				});
				downloadB.setDraw(new PUIPaintable() {
					@Override
					public void paint(Graphics g, int x, int y, int w, int h) {
						g.setColor(PUIElement.darkBG_1);
						g.fillRect(x, y, w, h);
						g.setColor(Color.green);
						g.fillRect(x, y, (int) (w * Math.max(0f, Math.min(1f, Main.downloadProgress))), h);
						g.setColor(PUIElement.darkOutline);
						g.drawRect(x, y, w, h);
						g.setColor(PUIElement.darkText);

						int[][] p = new int[2][7];
						p[0][0] = x;
						p[1][0] = y + h / 2;
						p[0][1] = x + w / 2;
						p[1][1] = y + h;
						p[0][2] = x + w;
						p[1][2] = y + h / 2;
						p[0][3] = x + w / 2 + w / 8;
						p[1][3] = y + h / 2;
						p[0][4] = x + w / 2 + w / 8;
						p[1][4] = y;
						p[0][5] = x + w / 2 - w / 8;
						p[1][5] = y;
						p[0][6] = x + w / 2 - w / 8;
						p[1][6] = y + h / 2;
						g.fillPolygon(p[0], p[1], p[0].length);
					}
				});

				uploadB = new PUIText(c);
				uploadB.addActionListener(new PUIAction() {
					@Override
					public void run(PUIElement e) {
						if (Main.isDownloading)
							return;
						if (Main.isUploading) {
							if (f.getUserConfirm("Cancel the uploadProcess?", "Cancel?"))
								Main.isUploading = false;
						}
						if (Main.sm != null && Main.sm.isConnected()) {
							if (Main.bb != null && Main.bb.getSize() != 0)
								Main.upload();
						}
					}
				});
				uploadB.setDraw(new PUIPaintable() {
					@Override
					public void paint(Graphics g, int x, int y, int w, int h) {
						g.setColor(PUIElement.darkBG_1);
						g.fillRect(x, y, w, h);
						g.setColor(Color.green);
						g.fillRect(x, y, (int) (w * Math.max(0f, Math.min(1f, Main.uploadProgress))), h);
						g.setColor(PUIElement.darkOutline);
						g.drawRect(x, y, w, h);
						g.setColor(PUIElement.darkText);

						int[][] p = new int[2][7];
						p[0][0] = x;
						p[1][0] = y + h / 2;
						p[0][1] = x + w / 2;
						p[1][1] = y;
						p[0][2] = x + w;
						p[1][2] = y + h / 2;
						p[0][3] = x + w / 2 + w / 8;
						p[1][3] = y + h / 2;
						p[0][4] = x + w / 2 + w / 8;
						p[1][4] = y + h;
						p[0][5] = x + w / 2 - w / 8;
						p[1][5] = y + h;
						p[0][6] = x + w / 2 - w / 8;
						p[1][6] = y + h / 2;
						g.fillPolygon(p[0], p[1], p[0].length);
					}
				});

				prevPage = new PUIElement(c);
				prevPage.addActionListener(new PUIAction() {
					@Override
					public void run(PUIElement that) {
						if (Main.currentPage != 0) {
							Main.currentPage--;
							f.updateElements();
						}
					}
				});
				prevPage.setDraw(new PUIPaintable() {
					@Override
					public void paint(Graphics g, int x, int y, int w, int h) {
						g.setColor(PUIElement.darkBG_2);

						int[][] p = new int[2][3];
						p[0][0] = x;
						p[1][0] = y + h / 2;
						p[0][1] = x + w;
						p[1][1] = y;
						p[0][2] = x + w;
						p[1][2] = y + h;
						g.fillPolygon(p[0], p[1], p[0].length);
					}
				});
				currentPageB = new PUIText(c, "-1");
				currentPageB.setDraw(null);
				currentPageB.setTextColor(PUIElement.darkText);
				nextPage = new PUIElement(c);
				nextPage.addActionListener(new PUIAction() {
					@Override
					public void run(PUIElement that) {
						Main.currentPage++;
						f.updateElements();
					}
				});
				nextPage.setDraw(new PUIPaintable() {
					@Override
					public void paint(Graphics g, int x, int y, int w, int h) {
						g.setColor(PUIElement.darkBG_2);

						int[][] p = new int[2][3];
						p[0][0] = x + w;
						p[1][0] = y + h / 2;
						p[0][1] = x;
						p[1][1] = y;
						p[0][2] = x;
						p[1][2] = y + h;
						g.fillPolygon(p[0], p[1], p[0].length);
					}
				});

				buttonMatrix = new ButtonMatrix(c);
				buttonMatrix.addActionListener(new PUIAction() {
					@Override
					public void run(PUIElement that) {
						buttonMatrix.setSelected();
						if (buttonMatrix.selectedIndex != -1)
							Main.selectedButtonIndex = buttonMatrix.selectedIndex + Main.currentPage * 16;
						else
							Main.selectedButtonIndex = -1;
						f.updateElements();
					}
				});

				for (int i = 0; i < boxInfos.length; i++) {
					boxInfos[i] = new PUIText(c);
					boxInfos[i].setDraw(null);
					boxInfos[i].setTextColor(Color.white);
				}

				keyInfo = new PUIText(c, "Key: ");
				keyInfo.setDraw(null);
				keyInfo.setTextColor(Color.white);

				// macros
				for (int i = 0; i < macros.length; i++) {
					macros[i] = new MacroButton(c, new PUIAction() {
						@Override
						public void run(PUIElement that) { // normalKey
							if (Main.selectedButtonIndex == -1)
								return;

							int index = f.getUserSelection("Key (Normal):", Main.keyActionNames_Normal);
							Button b = Main.getselectedButton();
							if (b != null) {
								b.values[(int) that.getMetadata()] = Main.keyActionValues_Normal.get(index);
								b.hasBeedEdited = true;
							}
							f.updateElements();
						}
					}, new PUIAction() {
						@Override
						public void run(PUIElement that) { // functionKey
							if (Main.selectedButtonIndex == -1)
								return;

							int index = f.getUserSelection("Key (Fuction):", Main.keyActionNames_Function);
							Button b = Main.getselectedButton();
							if (b != null) {
								b.values[(int) that.getMetadata()] = Main.keyActionValues_Function.get(index);
								b.hasBeedEdited = true;
							}
							f.updateElements();
						}
					}, new PUIAction() {
						@Override
						public void run(PUIElement that) { // customKey
							if (Main.selectedButtonIndex == -1)
								return;

							String s = f.getUserInput("Key (Custom KeyCode | Only Decimal):", "0");
							Button b = Main.getselectedButton();
							if (b != null) {
								int keycode = 0;
								try {
									keycode = Integer.parseInt(s);
								} catch (Exception e) {
								}
								b.values[(int) that.getMetadata()] = keycode;
								b.hasBeedEdited = true;
							}
							f.updateElements();
						}
					});
					macros[i].setMetadata(i);
					macros[i].addActionListener(new PUIAction() {
						@Override
						public void run(PUIElement that) {
							if (Main.selectedButtonIndex == -1)
								return;

							int index = f.getUserSelection("Key:", Main.keyActionNames_Normal);
							Button b = Main.getselectedButton();
							if (b != null) {
								b.values[(int) that.getMetadata()] = Main.keyActionValues_Normal.get(index);
								b.hasBeedEdited = true;
							}
							f.updateElements();
						}
					});
				}

				resetMacro = new PUIText(c, "R");
				resetMacro.addActionListener(new PUIAction() {
					@Override
					public void run(PUIElement that) {
						if (Main.selectedButtonIndex == -1)
							return;

						Button b = Main.getselectedButton();

						if (b != null)
							for (int i = 0; i < macros.length; i++) {
								b.values[i] = 0;
								b.hasBeedEdited = true;
							}
						f.updateElements();
					}
				});

				for (PUIElement e : PUIElement.registeredElements) {
					e.doPaintOverOnHover(false);
					e.doPaintOverOnPress(false);
				}
			}
		});
		f.addWindowListener(new WindowListener() {

			@Override
			public void windowClosing(WindowEvent e) {
				if (Main.sm != null) {
					Main.sm.close();
				}
			}

			@Override
			public void windowOpened(WindowEvent e) {
			}

			@Override
			public void windowIconified(WindowEvent e) {
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
			}

			@Override
			public void windowClosed(WindowEvent e) {
			}

			@Override
			public void windowActivated(WindowEvent e) {
			}
		});
		f.setUpdateElements(new PUIUpdatable() {
			@Override
			public void update(int w, int h) {

				// variables
				space = h / 100;
				leftMenuBarWidth = w / 4;
				defaultButtonSize = h / 8;
				if (defaultButtonSize * 2 > (leftMenuBarWidth - space * 2)) {
					defaultButtonSize = (leftMenuBarWidth - space * 2) / 2;
				}
				topMenuBarHeight = defaultButtonSize + space * 2;
				pageSettingsWidth = ((w - leftMenuBarWidth) / 2);

				buttonMatrixWidth = pageSettingsWidth - space * 2;
				if (buttonMatrixWidth + defaultButtonSize + space * 2 > h) {
					buttonMatrixWidth = h - defaultButtonSize - space * 3;
				}

				connectB.setBounds(space, space, leftMenuBarWidth - space * 2, defaultButtonSize);
				downloadB.setBounds(leftMenuBarWidth / 2 - defaultButtonSize, defaultButtonSize + space * 2,
						defaultButtonSize, defaultButtonSize);
				uploadB.setBounds(leftMenuBarWidth / 2, defaultButtonSize + space * 2, defaultButtonSize,
						defaultButtonSize);

				int textSize = PUIText.getTextWidth(defaultButtonSize, currentPageB.getText().length()) + 2;
				currentPageB.setBounds(leftMenuBarWidth + pageSettingsWidth / 2 - textSize / 2, space, textSize,
						defaultButtonSize);
				prevPage.setBounds((int) (currentPageB.getX() - (defaultButtonSize * 0.7)), space,
						(int) (defaultButtonSize * 0.7), defaultButtonSize);
				nextPage.setBounds((int) (currentPageB.getX() + textSize), space, (int) (defaultButtonSize * 0.7),
						defaultButtonSize);

				updateBoxInfos();

				int offset = 0;
				if (pageSettingsWidth - space * 2 > buttonMatrixWidth)
					offset = (pageSettingsWidth - space * 2 - buttonMatrixWidth) / 2;
				buttonMatrix.setBounds(leftMenuBarWidth + space + offset, defaultButtonSize + space * 2,
						buttonMatrixWidth, buttonMatrixWidth);

				keyInfo.setBounds(leftMenuBarWidth + pageSettingsWidth + space, space + defaultButtonSize / 2,
						pageSettingsWidth, defaultButtonSize / 2);

				int macroHeight = (int) (((h - (defaultButtonSize + space * 3) - defaultButtonSize) / macros.length)
						* 0.5);
				for (int i = 0; i < macros.length; i++) {
					macros[i].setBounds(leftMenuBarWidth + pageSettingsWidth + space,
							defaultButtonSize + space * 2 + macroHeight * i, pageSettingsWidth - space * 2,
							macroHeight);
				}

				resetMacro.setBounds(w - defaultButtonSize - space, h - defaultButtonSize - space, defaultButtonSize,
						defaultButtonSize);

				// arduino
				if (Main.sm != null && Main.sm.isConnected()) {
					connectB.setText(Main.sm.getPort());
				} else {
					connectB.setText("-");
				}

				// macro
				if (Main.sm != null && Main.bb != null) {
					if (Main.selectedButtonIndex != -1) {
						Button b = Main.bb.getButton(Main.selectedButtonIndex);
						if (b != null && b.values != null) {
							for (int i = 0; i < macros.length; i++) {
								macros[i].setText(Main.getKeyAction(b.values[i]));
							}
						}
					} else {
						for (int i = 0; i < macros.length; i++)
							macros[i].setText("");
					}
				}
			}
		});
		f.setDraw(new PUIPaintable() {
			@Override
			public void paint(Graphics g, int x, int y, int w, int h) {

				g.setColor(new Color(80, 80, 80));
				g.fillRect(leftMenuBarWidth, 0, pageSettingsWidth, h);

				currentPageB.setText((Main.currentPage + 1) + "");

				connectB.draw(g);
				uploadB.draw(g);
				prevPage.draw(g);
				nextPage.draw(g);
				downloadB.draw(g);
				buttonMatrix.draw(g);
				currentPageB.draw(g);
				resetMacro.draw(g);

				for (PUIText t : boxInfos)
					t.draw(g);

				keyInfo.draw(g);
				for (PUIText t : macros)
					t.draw(g);
			}
		});

		// MenuBar
		JMenuBar mb = new JMenuBar();
		JMenu m = new JMenu("Config");
		mb.add(m);

		JMenuItem exportButton = new JMenuItem("Export");
		exportButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (Main.bb != null) {
					System.out.println("export");

					JFileChooser folderLocation = new JFileChooser();
					folderLocation.removeChoosableFileFilter(folderLocation.getChoosableFileFilters()[0]);
					folderLocation.setFileFilter(new FileFilter() {
						@Override
						public String getDescription() {
							return "ButtonBox Config";
						}

						@Override
						public boolean accept(File f) {
							if (PFolder.isFolder(f.getAbsolutePath()))
								return true;
							return false;
						}
					});
					folderLocation.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					int returnVal = folderLocation.showOpenDialog(f.c());
					if (returnVal != JFileChooser.APPROVE_OPTION) {
						System.err.println("[UI] :: FileChooser canceled!");
						return;
					}

					File folderLoc = folderLocation.getSelectedFile();

					PDataStorage buttonConfig = new PDataStorage();

					int biggestIndex = 0;
					ArrayList<Button> buttons = new ArrayList<>();
					for (Button b : Main.bb.buttons)
						if (!b.isEmpty()) {
							buttons.add(b);
							if (b.index > biggestIndex)
								biggestIndex = b.index;
						}

					buttonConfig.add("s", biggestIndex);
					for (Button b : buttons) {
						String data = "";
						for (int i = 0; i < b.values.length; i++) {
							data += b.values[i];
							if (i != b.values.length - 1) { // add "," between values
								data += ",";
							}
						}

						buttonConfig.add("b" + b.index, data);
					}
					buttonConfig.save(folderLoc.getAbsolutePath() + "/ButtonBox.bbconfig");
				}
			}
		});
		m.add(exportButton);

		JMenuItem importButton = new JMenuItem("Import");
		importButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (Main.bb != null) {

						JFileChooser fileLocation = new JFileChooser();
						fileLocation.removeChoosableFileFilter(fileLocation.getChoosableFileFilters()[0]);
						fileLocation.setFileFilter(new FileFilter() {
							@Override
							public String getDescription() {
								return "ButtonBox Config";
							}

							@Override
							public boolean accept(File f) {
								if ((PFolder.isFile(f.getAbsolutePath())
										&& PFile.getFileType(f.getAbsolutePath()).equalsIgnoreCase("bbconfig"))
										|| PFolder.isFolder(f.getAbsolutePath()))
									return true;
								return false;
							}
						});
						fileLocation.setFileSelectionMode(JFileChooser.FILES_ONLY);
						int returnVal = fileLocation.showOpenDialog(f.c());
						if (returnVal != JFileChooser.APPROVE_OPTION) {
							System.err.println("[UI] :: FileChooser canceled!");
							return;
						}

						File folderLoc = fileLocation.getSelectedFile();
						System.out.println("[Config] :: " + folderLoc.getAbsolutePath());

						PDataStorage buttonConfig = new PDataStorage();
						buttonConfig.read(folderLoc.getAbsolutePath());

						int maxIndex = buttonConfig.getInteger("s");

						Main.bb.buttons.clear();
						for (int i = 0; i < maxIndex + 1; i++) {
							String data = buttonConfig.getString("b" + i);
							if (data != null) {

								ArrayList<Integer> D = getNumbers(data);
								if (D == null)
									continue;

								int d[] = new int[D.size()];
								for (int j = 0; j < D.size(); j++)
									d[j] = D.get(j);

								System.out.print("[Config] :: importing=" + i + " d=");
								for (int j = 0; j < d.length; j++)
									System.out.print(d[j] + ";");
								System.out.println();
								Main.bb.setButton(i, d).hasBeedEdited = true;
							}
						}

						f.updateElements();
						f.sendUserInfo("Successfully imported config!");
					}
				} catch (Exception exc) {
					f.updateElements();
					f.sendUserError("Something went wrong when loading config");
				}
			}
		});
		m.add(importButton);
		f.setJMenuBar(mb);
	}

	private ArrayList<Integer> getNumbers(String data) {
		ArrayList<Integer> out = new ArrayList<>();

		String temp = "";
		for (int i = 0; i < data.length(); i++) {
			if (data.charAt(i) == ',') {
				try {
					out.add(Integer.parseInt(temp));
					temp = "";
				} catch (Exception e) {
					return null;
				}
			} else {
				temp += data.charAt(i);
			}
		}
		try {
			out.add(Integer.parseInt(temp));
		} catch (Exception e) {
			return null;
		}

		return out;
	}

	private void updateBoxInfos() {
		updateBoxInfosText();
		int biggestStringSize = 1;
		for (PUIText t : boxInfos)
			if (t.getText() != null && t.getText().length() > biggestStringSize)
				biggestStringSize = t.getText().length();

		int boxInfoHeight = PUIText.getTextHeight(leftMenuBarWidth, biggestStringSize);
		for (int i = 0; i < boxInfos.length; i++) {
			boxInfos[i].setBounds(0, downloadB.getY() + downloadB.getH() + space + boxInfoHeight * i, leftMenuBarWidth,
					boxInfoHeight);
		}
	}

	private void updateBoxInfosText() {
		if (Main.bb != null) {
			boxInfos[0].setText("Version: " + Main.bb.getVersion());
			boxInfos[1].setText("Size: " + Main.bb.getSize());
			boxInfos[2].setText("Pages: " + Main.bb.getPages());
			boxInfos[3].setText("Buttons: " + Main.bb.getButtons());
			Main.bb.updateByteSize();
			boxInfos[4].setText("Bytes: " + Main.bb.getUsedBytes());
		} else {
			boxInfos[0].setText("Version: ");
			boxInfos[1].setText("Size: ");
			boxInfos[2].setText("Pages: ");
			boxInfos[3].setText("Buttons: ");
			boxInfos[4].setText("Bytes: ");
		}
	}

}
