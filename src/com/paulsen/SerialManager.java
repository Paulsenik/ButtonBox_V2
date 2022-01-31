package com.paulsen;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Scanner;

import com.fazecast.jSerialComm.SerialPort;

public class SerialManager {

	private boolean isConnected = false;
	private OutputStream out;
	private SerialPort port;

	private Recievable recieve;
	private Runnable onDisconnect;

	public static String[] getAllPorts() {
		SerialPort sp[] = SerialPort.getCommPorts();
		String s[] = new String[sp.length];
		for (int i = 0; i < sp.length; i++)
			s[i] = sp[i].getSystemPortName();
		return s;
	}

	public void setDisconnectAction(Runnable r) {
		onDisconnect = r;
	}
	
	public void connect(String portName) {
		try {
			port = SerialPort.getCommPort(portName);
		} catch (Exception e) {
			isConnected = false;
			return;
		}

		port.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
		port.openPort();
		isConnected = true;

		Scanner scnIn = new Scanner(port.getInputStream());
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (port.isOpen()) {
					try {
						String s = scnIn.nextLine();
						if (recieve != null)
							recieve.in(s);
					} catch (Exception e) {
					}
				}
				if (!port.isOpen()) {
					System.out.println("Disconnect");
					isConnected = false;
					if (onDisconnect != null)
						onDisconnect.run();
				}
				scnIn.close();

			}
		}).start();

		out = port.getOutputStream();
	}

	public void send(String data) {
		try {
			out.write((data.getBytes()));
			out.flush();
		} catch (IOException e) {
		}
	}

	public void close() {
		if (port != null)
			port.closePort();
	}

	public boolean isConnected() {
		return isConnected;
	}

	public void setRecieve(Recievable r) {
		recieve = r;
	}
	
	public String getPort() {
		return port.getSystemPortName();
	}

}
