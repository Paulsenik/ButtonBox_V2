package com.paulsen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.paulsen.ButtonBox.ArduinoEEPROM;
import com.paulsen.ButtonBox.Button;
import com.paulsen.io.CustomProtocol;
import com.paulsen.io.PFile;
import com.paulsen.io.PFolder;
import com.paulsen.ui.UI;

public class Main {

	public static final int INTSIZE = 2; // int in bytes
	public static final int MAXMACROSIZE = 4;

	public static int currentPage = 0, selectedButtonIndex = -1; // 0->Maxbuttonsize
	public static boolean isDownloading = false, isUploading = false;
	public static float downloadProgress = 0f, uploadProgress = 0f;
	public static volatile SerialManager sm;
	public static volatile ButtonBox bb;

	public static volatile ArduinoEEPROM eeprom;

	private static String versionOut;
	private static int sizeOut = -1;

	public static CustomProtocol buttonSet, get, reset, size, version, uploadSize;

	// download
	public static int bytesExpected = 0;
	public static long lastByteSent = 0;
	public static final int MAXTIMEOUT = 1000;

	public static ArrayList<String> keyActionNames_Normal, keyActionNames_Function;
	public static ArrayList<Integer> keyActionValues_Normal, keyActionValues_Function;

	public static UI ui;

	private static String SAVEFOLDER = "ButtonBox_V2_Programmer";
	private static boolean devMode = false; // TODO

	public static void main(String[] args) {

		if (!devMode) {

			if (!new File(SAVEFOLDER).exists())
				PFolder.createFolder(SAVEFOLDER);

			// Set Console out
			if (new File(SAVEFOLDER + "/consoleOut.txt").exists())
				new PFile(SAVEFOLDER + "/consoleOut.txt").delete();
			PrintStream out;
			try {
				out = new PrintStream(new FileOutputStream(SAVEFOLDER + "/consoleOut.txt"));
				System.setOut(out);
				System.setErr(out);
			} catch (FileNotFoundException e) {
			}
		}

		buttonSet = new CustomProtocol("b", '[', ']', "^<<^", "^>>^");
		get = new CustomProtocol("g", '[', ']', "^<<^", "^>>^");
		reset = new CustomProtocol("r", '[', ']', "^<<^", "^>>^");
		size = new CustomProtocol("s", '[', ']', "^<<^", "^>>^");
		version = new CustomProtocol("ButtonBox", '[', ']', "^<<^", "^>>^");
		uploadSize = new CustomProtocol("us", '[', ']', "^<<^", "^>>^");

		initKeyActions_Normal();
		initKeyActions_Function();

		sm = new SerialManager();

		new Timer().scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				if (isDownloading && lastByteSent + MAXTIMEOUT < System.currentTimeMillis())
					if (bytesExpected != 0) {
						ui.f.sendUserInfo("Could not download all bytes! " + bytesExpected);
						isDownloading = false;
					}
			}
		}, 0, 1);

		ui = new UI();
	}

	public static void initSerial(String port) {
		if (sm == null)
			return;

		if (sm.isConnected())
			sm.close();

		sm.connect(port);
		sm.setDisconnectAction(new Runnable() {
			@Override
			public void run() {
				bb = null;
				ui.f.sendUserInfo("Disconnected");
				ui.f.updateElements();
			}
		});
		if (sm != null && sm.isConnected()) {
			sm.setRecieve(new Recievable() {
				@Override
				public void in(String in) {
					lastByteSent = System.currentTimeMillis();

					System.out.println("Recieved: " + in);
					if (buttonSet.isPartOfProtocol(in)) {
						String message = buttonSet.getMessage(in);

						if (isDownloading) {
							int[] bytes = getByteData(message);

							if (eeprom != null) {
								eeprom.set(bytes[0], (byte) bytes[1]);
							}

							if (bytesExpected != 0) {
								bytesExpected--;
								if (bytesExpected == 0) {
									System.out.println("Down finish: " + in + " " + bytesExpected);
									isDownloading = false;
									downloadProgress = 0f;

									ArrayList<Button> buttons = eeprom.getAllButtons();
									if (buttons != null) {
										for (Button b : buttons) {
											System.out.print(b.index + " => ");
											for (int a : b.values)
												System.out.print(a + " ");
											System.out.println("");
										}
										bb.buttons = buttons;
									} else {
										System.err.println("Something went wrong");
									}

									Main.ui.f.updateElements();
								} else {
									downloadProgress = (float) (sizeOut - bytesExpected) / sizeOut;
									Main.ui.f.repaint();
								}
							} else {
								ui.f.sendUserInfo("Error when downloading bytes!");
								isDownloading = false;
								Main.ui.f.updateElements();
							}

						}
					} else if (size.isPartOfProtocol(in)) {
						String message = size.getMessage(in);
						try {
							sizeOut = Integer.parseInt(message);
							bytesExpected = sizeOut;
							System.out.println("byteExpected:" + bytesExpected);
						} catch (Exception e) {
						}
					} else if (version.isPartOfProtocol(in)) {
						String message = version.getMessage(in);
						versionOut = message;
					}

					if (versionOut != null && sizeOut != -1 && bb == null) {
						bb = new ButtonBox(versionOut, sizeOut);
						eeprom = bb.getEEPROMInstance();
						Main.ui.f.updateElements();
					}
				}
			});
		}
	}

	public static void upload() {
		if (!isUploading) {
			if (sm != null && bb != null) {
				isUploading = true;

				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							String[] s = checkForUnnecessaryOverrides(bb.getAllJavaOutputs());

							sm.send(uploadSize.getProtocolOutput("" + s.length));

							for (int i = 0; i < s.length; i++)
								if (s != null)
									System.out.println("Ready to Upload: " + s[i]);

							isUploading = true;
							for (int i = 0; i < s.length; i++) {

								if (s[i] == null)
									continue;

								if (!isUploading) {
									uploadProgress = 0f;
									System.out.println("break");
									sm.close();
									return;
								}
								System.out.println("Upload: " + s[i]);
								sm.send(s[i]);

								uploadProgress = (float) (i + 1) / s.length;
								ui.f.repaint();
							}
						} catch (Exception e) {
							sm.connect(sm.getPort());
						}
						uploadProgress = 0;
						isUploading = false;
						System.out.println("upload finished");
//						sm.send(reset.getProtocolOutput("ino"));
						bb = null;
						sm.close();
						ui.f.updateElements();
					}
				}).start();
			}
		}
	}

	public static String[] checkForUnnecessaryOverrides(ArrayList<String> in) {

		System.out.println("size prev: " + in.size());

		String out[] = new String[in.size()];
		int index = 0;

		for (String s : in) {
//			String m = Main.buttonSet.getMessage(s);
//			int[] d = Main.getByteData(m);
//			if (0 != d[1]) {
				out[index++] = s;
//				System.err.println(s);
//			}
		}

		System.out.println("size post: " + out.length);

		return out;
	}

	public static void download() {
		if (sm != null) {
			String out = get.getProtocolOutput("all");
			System.out.println("send " + out);
			lastByteSent = System.currentTimeMillis();
			isDownloading = true;
			sm.send(out);
		}
	}

	public static void getBoardInfo() {
		String out = get.getProtocolOutput("info");
		System.out.println("send " + out);
		sm.send(out);
	}

	public static Button getselectedButton() {
		if (bb != null && selectedButtonIndex != -1) {
			return bb.getButton(selectedButtonIndex);
		}
		return null;
	}

	// int[0]->address, int[1]->value
	public static int[] getByteData(String message) {
		int a = 0, v = 0;
		String temp = "";

		try {
			for (int i = 0; i < message.length(); i++) {
				if (message.charAt(i) == ',') {
					a = Integer.parseInt(temp);
					temp = "";
				} else {
					temp += message.charAt(i);
				}
			}
			v = Integer.parseInt(temp);
		} catch (Exception e) {
			return null;
		}

		int b[] = { a, v };
		return b;
	}

	public static String getKeyAction(int decimalValue) {
		for (int i = 0; i < keyActionValues_Normal.size(); i++)
			if (keyActionValues_Normal.get(i) == decimalValue)
				return keyActionNames_Normal.get(i);
		for (int i = 0; i < keyActionValues_Function.size(); i++)
			if (keyActionValues_Function.get(i) == decimalValue)
				return keyActionNames_Function.get(i);
		return "" + decimalValue;
	}

	public static void initKeyActions_Normal() {
		ArrayList<String> n = new ArrayList<String>();
		ArrayList<Integer> v = new ArrayList<Integer>();
		//

		// EMPTY / DO_NOTHING
		n.add(" ");
		v.add(0);

		// ALPHABET
		for (int i = 33; i < 127; i++) {
			n.add("" + ((char) i));
			v.add(i);
		}

		//
		keyActionNames_Normal = n;
		keyActionValues_Normal = v;
	}

	public static void initKeyActions_Function() {
		ArrayList<String> n = new ArrayList<String>();
		ArrayList<Integer> v = new ArrayList<Integer>();
		//

		// EMPTY / DO_NOTHING
		n.add(" ");
		v.add(0); // 44

		// L
		n.add("L-CTRL");
		v.add(128);
		n.add("L-SHIFT");
		v.add(129);
		n.add("L-ALT");
		v.add(130);
		n.add("L-WIN");// L-GUI
		v.add(131);
		// R
		n.add("R-CTRL");
		v.add(132);
		n.add("R-SHIFT");
		v.add(133);
		n.add("R-ALT-GR");
		v.add(134);
		n.add("R-WIN");// R-GUI
		v.add(135);
		// ARROW
		n.add("ARROW-UP");
		v.add(218);
		n.add("ARROW-DOWN");
		v.add(217);
		n.add("ARROW-LEFT");
		v.add(216);
		n.add("ARROW-RIGHT");
		v.add(215);
		// MISC
		n.add("SPACE");
		v.add(32);
		n.add("BACKSPACE");
		v.add(178);
		n.add("TAB");
		v.add(179);
		n.add("ENTER/RETURN");
		v.add(176);
		n.add("ESC");
		v.add(177);
		n.add("INSERT");
		v.add(209);
		n.add("DELETE");
		v.add(212);
		n.add("PAGE-UP");
		v.add(211);
		n.add("PAGE-DOWN");
		v.add(214);
		n.add("HOME");
		v.add(210);
		n.add("END");
		v.add(213);
		n.add("CAPS-LOCK");
		v.add(193);
//		n.add("PrintScrn");
//		v.add(44);
		n.add("Scrl-Lock");
		v.add(145);

		// NUMPAD
		n.add("NUM-Lock");
		v.add(144);
//		n.add("NUM-0");
//		v.add(96);
//		n.add("NUM-1");
//		v.add(97);
//		n.add("NUM-2");
//		v.add(98);
//		n.add("NUM-3");
//		v.add(99);
//		n.add("NUM-4");
//		v.add(100);
//		n.add("NUM-5");
//		v.add(101);
//		n.add("NUM-6");
//		v.add(102);
//		n.add("NUM-7");
//		v.add(103);
//		n.add("NUM-8");
//		v.add(104);
//		n.add("NUM-9");
//		v.add(105);
//		n.add("NUM -");
//		v.add(109);
//		n.add("NUM +");
//		v.add(107);
//		n.add("NUM *");
//		v.add(106);
//		n.add("NUM /");
//		v.add(111);
//		n.add("NUM .");
//		v.add(110);

		// FUNCTION
		n.add("F1");
		v.add(194);
		n.add("F2");
		v.add(195);
		n.add("F3");
		v.add(196);
		n.add("F4");
		v.add(197);
		n.add("F5");
		v.add(198);
		n.add("F6");
		v.add(199);
		n.add("F7");
		v.add(200);
		n.add("F8");
		v.add(201);
		n.add("F9");
		v.add(202);
		n.add("F10");
		v.add(203);
		n.add("F11");
		v.add(204);
		n.add("F12");
		v.add(205);

		keyActionNames_Function = n;
		keyActionValues_Function = v;
	}

}
