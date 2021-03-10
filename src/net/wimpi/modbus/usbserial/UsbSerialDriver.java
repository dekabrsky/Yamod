/* Copyright 2011 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: http://code.google.com/p/usb-serial-for-android/
 */

package net.wimpi.modbus.usbserial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.wimpi.modbus.facade.IModbusLogger;
import net.wimpi.modbus.util.HexDump;

//import com.rockwell.modbus800.MainActivity;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;

/**
 * Driver interface for a supported USB serial device.
 * 
 * @author mike wakerly (opensource@hoho.com)
 */
public abstract class UsbSerialDriver {

	public static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
	public static final int DEFAULT_WRITE_BUFFER_SIZE = 16 * 1024;

	private static final int READ_WAIT_MILLIS = 200;

	protected final UsbDevice mDevice;
	protected final UsbDeviceConnection mConnection;

	protected final Object mReadBufferLock = new Object();
	protected final Object mWriteBufferLock = new Object();

	protected final SerialInputStream mInputStream;
	protected final SerialOutputStream mOutputStream;

	/** Internal read buffer. Guarded by {@link #mReadBufferLock}. */
	protected byte[] mReadBuffer;

	/** Internal write buffer. Guarded by {@link #mWriteBufferLock}. */
	protected byte[] mWriteBuffer;

	protected IModbusLogger m_logger;

	public UsbSerialDriver(UsbDevice device, UsbDeviceConnection connection) {
		mDevice = device;
		mConnection = connection;

		mReadBuffer = new byte[DEFAULT_READ_BUFFER_SIZE];
		mWriteBuffer = new byte[DEFAULT_WRITE_BUFFER_SIZE];

		mInputStream = new SerialInputStream();
		mOutputStream = new SerialOutputStream();
	}

	/**
	 * Opens and initializes the device as a USB serial device. Upon success,
	 * caller must ensure that {@link #close()} is eventually called.
	 * 
	 * @throws IOException
	 *             on error opening or initializing the device.
	 */
	public abstract void open() throws IOException;

	/**
	 * Closes the serial device.
	 * 
	 * @throws IOException
	 *             on error closing the device.
	 */
	public abstract void close() throws IOException;

	public void setLogger(IModbusLogger logger) {
		m_logger = logger;
	}

	public InputStream getInputStream() {
		return mInputStream;
	}

	public OutputStream getOutputStream() {
		return mOutputStream;
	}

	/**
	 * Reads as many bytes as possible into the destination buffer.
	 * 
	 * @param dest
	 *            the destination byte buffer
	 * @param timeoutMillis
	 *            the timeout for reading
	 * @return the actual number of bytes read
	 * @throws IOException
	 *             if an error occurred during reading
	 */
	public abstract int read(final byte[] dest, final int timeoutMillis)
			throws IOException;

	/**
	 * Writes as many bytes as possible from the source buffer.
	 * 
	 * @param src
	 *            the source byte buffer
	 * @param timeoutMillis
	 *            the timeout for writing
	 * @return the actual number of bytes written
	 * @throws IOException
	 *             if an error occurred during writing
	 */
	public abstract int write(final byte[] src, final int timeoutMillis)
			throws IOException;

	/**
	 * Sets the baud rate of the serial device.
	 * 
	 * @param baudRate
	 *            the desired baud rate, in bits per second
	 * @return the actual rate set
	 * @throws IOException
	 *             on error setting the baud rate
	 */
	public abstract int setBaudRate(final int baudRate) throws IOException;

	/**
	 * Returns the currently-bound USB device.
	 * 
	 * @return the device
	 */
	public final UsbDevice getDevice() {
		return mDevice;
	}

	/**
	 * Sets the size of the internal buffer used to exchange data with the USB
	 * stack for read operations. Most users should not need to change this.
	 * 
	 * @param bufferSize
	 *            the size in bytes
	 */
	public final void setReadBufferSize(int bufferSize) {
		synchronized (mReadBufferLock) {
			if (bufferSize == mReadBuffer.length) {
				return;
			}
			mReadBuffer = new byte[bufferSize];
		}
	}

	/**
	 * Sets the size of the internal buffer used to exchange data with the USB
	 * stack for write operations. Most users should not need to change this.
	 * 
	 * @param bufferSize
	 *            the size in bytes
	 */
	public final void setWriteBufferSize(int bufferSize) {
		synchronized (mWriteBufferLock) {
			if (bufferSize == mWriteBuffer.length) {
				return;
			}
			mWriteBuffer = new byte[bufferSize];
		}
	}

	public class SerialInputStream extends InputStream {
		private Exception mLastException;

		final byte[] mBuffer = new byte[1024];
		int mIndex = 0;
		int mLength = 0;

		public synchronized int read() throws IOException {
			if (mIndex < mLength) {

				if (m_logger != null)
					m_logger.log("Data read from SerialInputStream0: "
							+ HexDump.dumpHexString(mBuffer, mIndex, 1));

				return mBuffer[mIndex++];
			} else {
				mLength = UsbSerialDriver.this.read(mBuffer, READ_WAIT_MILLIS);
				mIndex = 0;

				if (m_logger != null)
					m_logger.log("Data read from serial port: "
							+ HexDump.dumpHexString(mBuffer, 0, mLength));

				if (mLength > 0)
					return read();
				else
					return -1;
			}
		}

		public synchronized int read(byte b[]) throws IOException {
			if (mIndex < mLength) {
				int bytes = Math.min(b.length, mLength - mIndex);
				System.arraycopy(mBuffer, mIndex, b, 0, bytes);
				mIndex += bytes;

				if (m_logger != null)
					m_logger.log("Data read from SerialInputStream1: "
							+ HexDump.dumpHexString(b, 0, bytes));

				return bytes;
			} else {
				mLength = UsbSerialDriver.this.read(mBuffer, READ_WAIT_MILLIS);
				mIndex = 0;

				if (m_logger != null)
					m_logger.log("Data read from serial port: "
							+ HexDump.dumpHexString(mBuffer, 0, mLength));

				return read(b);
			}
		}

		public synchronized int read(byte b[], int off, int length)
				throws IOException {
			if (mIndex < mLength) {
				int bytes = Math.min(length, mLength - mIndex);
				System.arraycopy(mBuffer, mIndex, b, off, bytes);
				mIndex += bytes;

				if (m_logger != null)
					m_logger.log("Data read from SerialInputStream2: "
							+ HexDump.dumpHexString(b, off, bytes));

				return bytes;
			} else {
				mLength = UsbSerialDriver.this.read(mBuffer, READ_WAIT_MILLIS);
				mIndex = 0;

				if (m_logger != null)
					m_logger.log("Data read from serial port: "
							+ HexDump.dumpHexString(mBuffer, 0, mLength));

				return read(b, off, length);
			}
		}

		@Override
		public int available() {
			return mLength - mIndex;
		}

		public synchronized void reset() throws IOException {
			mLength = 0;
			mIndex = 0;

			super.reset();
		}
	}

	public class SerialOutputStream extends OutputStream {
		private final byte[] mOneByte = new byte[1];

		public void write(int b) throws IOException {
			mOneByte[0] = (byte) b;
			UsbSerialDriver.this.write(mOneByte, READ_WAIT_MILLIS);

			if (m_logger != null)
				m_logger.log("Data to write from SerialOutputStream0: "
						+ HexDump.dumpHexString(mOneByte));
		}

		public void write(byte b[]) throws IOException {
			if (m_logger != null)
				m_logger.log("Data to write from SerialOutputStream1: "
						+ HexDump.dumpHexString(b, 0, b.length));

			UsbSerialDriver.this.write(b, READ_WAIT_MILLIS);
		}

		public void write(byte b[], int off, int len) throws IOException {
			if (m_logger != null) {
				m_logger.log("Data to write from SerialOutputStream2: off: "
						+ Integer.toString(off) + " len: "
						+ Integer.toString(len) + ": "
						+ HexDump.dumpHexString(b, off, len));
			}

			byte[] bb = new byte[len];
			System.arraycopy(b, off, bb, 0, len);
			UsbSerialDriver.this.write(bb, READ_WAIT_MILLIS);
		}
	}

}
