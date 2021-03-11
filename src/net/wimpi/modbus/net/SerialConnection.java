/***
 * Copyright 2002-2010 jamod development team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***/

package net.wimpi.modbus.net;

import net.wimpi.modbus.facade.IModbusLogger;
import net.wimpi.modbus.io.*;
import net.wimpi.modbus.usbserial.UsbSerialParameters;
import net.wimpi.modbus.util.SerialParameters;
import net.wimpi.modbus.usbserial.UsbSerialDriver;
import net.wimpi.modbus.usbserial.UsbSerialProber;

import java.io.IOException;
import java.io.InputStream;

import android.hardware.usb.UsbManager;

/**
 * Class that implements a serial connection which can be used for master and
 * slave implementations.
 * 
 * @author Dieter Wimberger
 * @author John Charlton
 * @version @version@ (@date@)
 */
public class SerialConnection implements MasterConnection {

	private SerialParameters m_Parameters;
	private UsbSerialParameters u_Parameters;

	private ModbusSerialTransport m_Transport;
	private boolean m_Open;
	private InputStream m_SerialIn;
	private IModbusLogger m_logger;
	
	/**
	 * The device currently in use, or {@code null}.
	 */
	private UsbSerialDriver m_SerialDevice;

	/**
	 * The system's USB service.
	 */
	private UsbManager m_UsbManager;

	/**
	 * Creates a SerialConnection object and initilizes variables passed in as
	 * params.
	 * 
	 * @param parameters
	 *            A SerialParameters object.
	 */
	public SerialConnection(UsbManager UsbManager, UsbSerialParameters parameters) {
		m_UsbManager = UsbManager;
		u_Parameters = parameters;
		m_Open = false;

		m_Transport = new ModbusRTUTransport();
	}// constructor

	public SerialConnection(SerialParameters serialParameters) {
		m_Parameters = serialParameters;
		m_Open = false;

		m_Transport = new ModbusRTUTransport();
	}

	/**
	 * Returns the <tt>ModbusTransport</tt> instance to be used for receiving
	 * and sending messages.
	 * 
	 * @return a <tt>ModbusTransport</tt> instance.
	 */
	public ModbusTransport getModbusTransport() {
		return m_Transport;
	}// getModbusTransport

	public void setLogger(IModbusLogger logger) {
		m_logger = logger;
		
		if (m_SerialDevice != null) {
			m_SerialDevice.setLogger(m_logger);
		}
	}
	
	/**
	 * Opens the communication port.
	 * 
	 * @throws Exception
	 *             if an error occurs.
	 * @param defaultTimeout
	 */
	public void connect(int defaultTimeout) throws Exception {
		m_SerialDevice = UsbSerialProber.acquire(m_UsbManager);
		// Log.d(TAG, "Resumed, mSerialDevice=" + m_SerialDevice);
		if (m_SerialDevice != null) {
			try {
				m_SerialDevice.open();
				m_SerialDevice.setLogger(m_logger);
				
				m_Transport.setSerialDevice(m_SerialDevice);
			} catch (IOException e) {
				try {
					m_SerialDevice.close();
				} catch (IOException e2) {
					// Ignore.
				}
				m_SerialDevice = null;
				return;
			}
		}
		else {
			throw new Exception("No serial device");
		}
		m_Open = true;
	}// open

	public void setReceiveTimeout(int ms) {
		// Set receive timeout to allow breaking out of polling loop during
		// input handling.
		/*
		 * try { m_SerialPort.enableReceiveTimeout(ms); } catch
		 * (UnsupportedCommOperationException e) { if(Modbus.debug)
		 * System.out.println(e.getMessage()); }
		 */
	}// setReceiveTimeout

	/**
	 * Sets the connection parameters to the setting in the parameters object.
	 * If set fails return the parameters object to origional settings and throw
	 * exception.
	 * 
	 * @throws Exception
	 *             if the configured parameters cannot be set properly on the
	 *             port.
	 */
	public void setConnectionParameters() throws Exception {
		/*
		 * // Save state of parameters before trying a set. int oldBaudRate =
		 * m_SerialPort.getBaudRate(); int oldDatabits =
		 * m_SerialPort.getDataBits(); int oldStopbits =
		 * m_SerialPort.getStopBits(); int oldParity = m_SerialPort.getParity();
		 * int oldFlowControl = m_SerialPort.getFlowControlMode();
		 * 
		 * // Set connection parameters, if set fails return parameters object
		 * // to original state. try {
		 * m_SerialPort.setSerialPortParams(m_Parameters.getBaudRate(),
		 * m_Parameters.getDatabits(), m_Parameters.getStopbits(),
		 * m_Parameters.getParity()); } catch (UnsupportedCommOperationException
		 * e) { m_Parameters.setBaudRate(oldBaudRate);
		 * m_Parameters.setDatabits(oldDatabits);
		 * m_Parameters.setStopbits(oldStopbits);
		 * m_Parameters.setParity(oldParity); if(Modbus.debug)
		 * System.out.println(e.getMessage());
		 * 
		 * throw new Exception("Unsupported parameter"); }
		 * 
		 * // Set flow control. try {
		 * m_SerialPort.setFlowControlMode(m_Parameters.getFlowControlIn() |
		 * m_Parameters.getFlowControlOut()); } catch
		 * (UnsupportedCommOperationException e) { if(Modbus.debug)
		 * System.out.println(e.getMessage());
		 * 
		 * throw new Exception("Unsupported flow control"); }
		 */
	}// setConnectionParameters

	@Override
	public void connect() throws Exception {
		connect(1000);
	}


	@Override
	public boolean isConnected() {
		return m_Open;
	}

	/**
	 * Close the port and clean up associated elements.
	 */
	public void close() {
		if (m_SerialDevice != null) {
			try {
				m_SerialDevice.close();
			} catch (IOException e) {
				// Ignore.
			}
			m_SerialDevice = null;
		}

		m_Open = false;
	}// close

	/**
	 * Reports the open status of the port.
	 * 
	 * @return true if port is open, false if port is closed.
	 */
	public boolean isOpen() {
		return m_Open;
	}// isOpen


}// class SerialConnection
