package net.wimpi.modbus.usbserial;

public interface SerialPort {
	public static final int DATABITS_5 = 1;
	public static final int DATABITS_6 = 2;
	public static final int DATABITS_7 = 3;
	public static final int DATABITS_8 = 4;
	public static final int FLOWCONTROL_NONE = 1;
	public static final int FLOWCONTROL_RTSCTS_IN = 2;
	public static final int FLOWCONTROL_RTSCTS_OUT = 3;
	public static final int FLOWCONTROL_XONXOFF_IN = 4;
	public static final int FLOWCONTROL_XONXOFF_OUT = 5;
	public static final int PARITY_EVEN = 1;
	public static final int PARITY_MARK = 2;
	public static final int PARITY_NONE = 3;
	public static final int PARITY_ODD = 4;
	public static final int PARITY_SPACE = 5;
	public static final int STOPBITS_1 = 1;
	public static final int STOPBITS_1_5 = 2;
	public static final int STOPBITS_2 = 3;
}
