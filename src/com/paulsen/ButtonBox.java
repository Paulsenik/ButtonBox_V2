package com.paulsen;

import java.util.ArrayList;
import java.util.Comparator;

public class ButtonBox {

	public class ArduinoEEPROM {

		private byte[] bytes;
		private boolean[] filled;

		public ArduinoEEPROM(int size) {
			System.err.println("NEW EEPROM\n\n");
			bytes = new byte[size];
			filled = new boolean[size];
			for (int i = 0; i < filled.length; i++) {
				filled[i] = false;
			}
		}

		public void set(int address, byte value) {
			bytes[address] = value;
//			if (filled[address]) {
//				System.err.println("Some error Occured");
//				return;
//			}
			filled[address] = true;
//			System.out.println("set:" + address + " " + ((int) value) + "/" + value + " " + filled[address]);
		}

		public byte get(int address) {
			return bytes[address];
		}

		public boolean isComplete() {
			for (int i = 0; i < filled.length; i++)
				if (!filled[i]) {
					return false;
				}
			return true;
		}

		public ArrayList<Button> getAllButtons() {
			ArrayList<Button> buttons = new ArrayList<>();

			int buttonIndex = 0;
			for (int i = 0; i < bytes.length; i++) {
				int bLength = bytes[i];

				if (bLength != 0) { // jump over macroLength when there is a macro comming ( length != 0 )
					i++;
				}

				if (bLength % 2 != 0)
					return null;

				if (bLength != 0) {
					int[] values = new int[bLength / 2];
					for (int j = 0; j < values.length; j++) {
						values[j] = convertToInt(bytes[i + 1 + j * Main.INTSIZE], bytes[i + j * Main.INTSIZE]);
					}
					Button b = new Button(buttonIndex++, values);
					if (!b.isEmpty())
						buttons.add(b);
					i += bLength - 1;
				} else {
					int[] values = { 0 };
					buttons.add(new Button(buttonIndex++, values));
				}

			}
			return buttons;
		}
	}

	public class Button {
		public int index;
		public int values[];
		public boolean hasBeedEdited = false;

		public Button(int index, int value[]) {
			int a[] = new int[Main.MAXMACROSIZE];
			for (int i = 0; i < value.length; i++)
				a[i] = value[i];

			this.index = index;
			this.values = a;
		}

		public int getByteCount() {
			if (isEmpty())
				return 1;
			int length = 0;
			for (int i = 0; i < values.length; i++)
				if (values[i] != 0)
					length++;
			return length * 2 + 1;
		}

		public boolean isEmpty() {
			if (values != null)
				for (int i : values)
					if (i != 0)
						return false;
			return true;
		}
	}

	private String version;
	private int size, usedBytes;

	public ArrayList<Button> buttons = new ArrayList<>();

	public ButtonBox(String version, int size) {
		this.version = version;
		this.size = size;
	}

	public ArduinoEEPROM getEEPROMInstance() {
		return new ArduinoEEPROM(size);
	}

	/**
	 * Adds or modifies existing Buttons
	 * 
	 * @param index of Button
	 * @param value of Button
	 */
	public Button setButton(int index, int[] values) {
		if (index < 0 || index > size)
			return null;
		Button b = getButton(index);
		if (b == null) {
			b = new Button(index, values);
			buttons.add(b);
		} else
			b.values = values;

		updateByteSize();

		return b;
	}

	public Button setButton(int index) {
		for (int i = 0; i < buttons.size(); i++)
			if (buttons.get(i).index == index)
				return buttons.get(i);

		if (index < 0 || index > size)
			return null;
		int a[] = new int[Main.MAXMACROSIZE];
		Button b = new Button(index, a);
		buttons.add(b);
		updateByteSize();
		return b;
	}

	public synchronized void updateByteSize() {
		usedBytes = 0;
		boolean hasReachedEnd = false;
		for (int i = buttons.size() - 1; i >= 0; i--)
			if (hasReachedEnd || !buttons.get(i).isEmpty()) {
				hasReachedEnd = true;
				usedBytes += buttons.get(i).getByteCount();
			}
	}

	public Button getButton(int index) {
		for (Button b : buttons)
			if (b.index == index)
				return b;
		return setButton(index);
	}

	public void sortButtons() {
		buttons.sort(new Comparator<Button>() {
			@Override
			public int compare(Button b1, Button b2) {
				if (b1.index < b2.index)
					return -1;
				if (b1.index > b2.index)
					return 1;
				System.err.println("No multiple Buttons allowed!");
				return 0; // ==
			}
		});
	}

	// fills missing buttons between existing ones
	private void fillButtonHoles() {
		int biggestButtonIndex = 0;
		boolean hasOneButtonBeenEdited = false;
		for (int i = 0; i < buttons.size(); i++) {

			if (!hasOneButtonBeenEdited) {
				if (buttons.get(i).hasBeedEdited)
					hasOneButtonBeenEdited = true;
			} else {
//				System.err.println("edited bindex=" + buttons.get(i).index);
				buttons.get(i).hasBeedEdited = true;
			}

			if (!buttons.get(i).isEmpty() && buttons.get(i).index > biggestButtonIndex)
				biggestButtonIndex = buttons.get(i).index;
		}

		for (int i = 0; i < biggestButtonIndex; i++)
			setButton(i);
	}

	public ArrayList<String> getAllJavaOutputs() {
		fillButtonHoles();
		sortButtons();

		ArrayList<String> outputs = new ArrayList<String>();

		int currentAddress = 0;
		for (Button b : buttons) {
			int bytes = b.getByteCount();

			if (b.hasBeedEdited) {
				outputs.addAll(getJavaOutputs(currentAddress, b.values));
			}

			if (!b.isEmpty())
				usedBytes = currentAddress;

			currentAddress += bytes;
		}
		return outputs;
	}

	/**
	 * 
	 * @param startIndex
	 * @param macro-keys
	 * @return
	 */
	public static ArrayList<String> getJavaOutputs(int startIndex, int... a) {
		ArrayList<String> out = new ArrayList<String>();

		ArrayList<Integer> values = new ArrayList<Integer>();
		for (int i = 0; i < a.length; i++)
			if (a[i] != 0)
				values.add(a[i]);

		out.add(toConsole(startIndex, values.size() * Main.INTSIZE));

		for (int i = 0; i < values.size(); i++) {
			out.add(toConsole(startIndex + 1 + i * Main.INTSIZE, values.get(i)));
		}
		return out;
	}

	public static String toConsole(int address, int value) {
		return "b[" + address + "," + value + "]";
	}

	public String getVersion() {
		return version;
	}

	public int getSize() {
		return size;
	}

	public int getPages() {
		int highestIndex = 0;
		for (Button b : buttons) {
			if (!b.isEmpty() && b.index > highestIndex)
				highestIndex = b.index;
		}
		return ((highestIndex+16) / 16);
	}

	public int getUsedBytes() {
		return usedBytes;
	}

	public int getButtons() {
		int i = 0;
		for (Button b : buttons)
			if (!b.isEmpty())
				i++;
		return i;
	}

	public static int convertToInt(byte a, byte b) {
		return (int) ((a & 0xff) << 8) | (b & 0xff);
	}

}
