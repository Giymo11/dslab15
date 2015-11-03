package shared;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads commands from an {@link InputStream}, executes them and writes the
 * result to a {@link OutputStream}.
 */
public class Shell extends CommandInterpreter {

	private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("HH:mm:ss.SSS");
		}
	};

	private String name;

	/**
	 * Creates a new {@code Shell} instance.
	 *
	 * @param name
	 *            the name of the {@code Shell} displayed in the prompt
	 * @param in
	 *            the {@code InputStream} to read messages from
	 * @param out
	 *            the {@code OutputStream} to write messages to
	 */
	public Shell(String name, InputStream in, OutputStream out) {
		super(in, out);
		this.name = name;
	}

	/**
	 * Executes commands read from the provided {@link InputStream} and prints
	 * the output.
	 * <p/>
	 * Note that this method blocks until either
	 * <ul>
	 * <li>This {@code Shell} is closed,</li>
	 * <li>the end of the {@link InputStream} is reached,</li>
	 * <li>or an {@link IOException} is thrown while reading from or writing to
	 * the streams.</li>
	 * </ul>
	 */
	@Override
	public void run() {
		try {
			for (String line; !Thread.currentThread().isInterrupted()
					&& (line = readLine()) != null;) {
				write( String.format("%s\t\t%s> %s%n", DATE_FORMAT.get().format(new Date()), name, line).getBytes() );
				Object result;
				try {
					result = invoke(line);
				} catch (Throwable throwable) {
					ByteArrayOutputStream str = new ByteArrayOutputStream(1024);
					throwable.printStackTrace(new PrintStream(str, true));
					result = str.toString();
				}
				if (result != null) {
					print(result);
				}
			}
		} catch (IOException e) {
			try {
				writeLine("Shell closed");
			} catch (IOException ex) {
				System.out.println(ex.getClass().getName() + ": "
						+ ex.getMessage());
			}
		}
	}

	/**
	 * Writes the given line to the provided {@link OutputStream}.<br/>
	 *
	 * @param line
	 *            the line to write
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void writeLine(String line) throws IOException {
		String now = DATE_FORMAT.get().format(new Date());
		if (line.indexOf('\n') >= 0 && line.indexOf('\n') < line.length() - 1) {
			write((String.format("%s\t\t%s:\n", now, name)).getBytes());
			for (String l : line.split("[\\r\\n]+")) {
				super.writeLine(String.format("%s\t\t%s", now, l));
			}
		} else {
			super.writeLine((String.format("%s\t\t%s: %s", now, name, line)));
		}
	}



}
