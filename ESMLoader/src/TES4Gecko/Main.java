package TES4Gecko;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main
{
	public static Color backgroundColor = new Color(240, 240, 240);

	public static JFrame mainWindow;

	public static String installPath;

	public static String dataPath;

	public static String pluginDirectory;

	public static String fileSeparator;

	public static String lineSeparator;

	public static File propFile;

	public static PluginSpill pluginSpill;

	public static Properties properties;

	public static boolean debugMode = false;

	public static long maxMemory;

	private static String deferredText;

	private static Throwable deferredException;

	public static void main(String[] args)
	{
		try
		{
			String debugString = System.getProperty("debug.plugin");
			if ((debugString != null) && (debugString.equals("1")))
			{
				debugMode = true;
			}

			maxMemory = Runtime.getRuntime().maxMemory();
			if (debugMode)
			{
				System.out.println("Java has " + maxMemory / 1048576L + "MB of storage available");
			}

			lineSeparator = System.getProperty("line.separator");
			fileSeparator = System.getProperty("file.separator");

			dataPath = System.getProperty("user.home") + fileSeparator + "Local Settings" + fileSeparator + "Application Data"
					+ fileSeparator + "Oblivion";
			if (debugMode)
			{
				System.out.printf("Application data path: %s\n", new Object[]
				{ dataPath });
			}

			File dirFile = new File(dataPath);
			if (!dirFile.exists())
			{
				dirFile.mkdirs();
			}

			propFile = new File(dataPath + fileSeparator + "TES4Gecko.properties");
			properties = new Properties();
			if (propFile.exists())
			{
				FileInputStream in = new FileInputStream(propFile);
				properties.load(in);
				in.close();
			}

			installPath = System.getProperty("Oblivion.install.path");
			if (installPath == null)
			{
				installPath = properties.getProperty("install.directory");
				if (installPath == null)
				{
					String regString = "reg query \"HKLM\\Software\\Bethesda Softworks\\Oblivion\" /v \"Installed Path\"";
					Process process = Runtime.getRuntime().exec(regString);
					StreamReader streamReader = new StreamReader(process.getInputStream());
					streamReader.start();
					process.waitFor();
					streamReader.join();
					String line;
					while ((line = streamReader.getLine()) != null)
					{

						int sep = line.indexOf("REG_SZ");
						if (sep >= 0)
						{
							installPath = line.substring(sep + 6).trim();
							break;
						}
					}

					if (installPath == null)
					{
						JFileChooser fc = new JFileChooser();
						fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

						fc.setDialogTitle("Set Oblvion Dir Now");
						int result = fc.showOpenDialog(null);
						if (result == JFileChooser.APPROVE_OPTION)
						{
							File sf = fc.getSelectedFile();
							installPath = sf.getAbsolutePath();
						}
						else
						{
							throw new IOException("Unable to locate Oblivion installation directory");
						}
					}
				}
			}
			properties.setProperty("install.directory", installPath);

			pluginDirectory = properties.getProperty("plugin.directory");
			if (pluginDirectory == null)
			{
				pluginDirectory = installPath + "Data";
				properties.setProperty("plugin.directory", pluginDirectory);
			}

			String tempPath = System.getenv("TEMP");
			if ((tempPath == null) || (tempPath.length() == 0))
			{
				tempPath = dataPath;
			}
			if (debugMode)
			{
				System.out.printf("Temporary data path: %s\n", new Object[]
				{ tempPath });
			}
			File spillFile = new File(tempPath + fileSeparator + "Gecko.spill");
			pluginSpill = new PluginSpill(spillFile, maxMemory / 10L);

			if (debugMode)
			{
				System.out.println("Starting the Swing GUI");
			}
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					Main.createAndShowGUI();
				}
			});
		}
		catch (Exception exc)
		{
			logException("Exception during program initialization", exc);
		}
	}

	private static void createAndShowGUI()
	{
		try
		{
			JFrame.setDefaultLookAndFeelDecorated(true);
			mainWindow = new MainWindow();
			mainWindow.pack();
			mainWindow.setVisible(true);
		}
		catch (Exception exc)
		{
			logException("Exception during GUI initialization", exc);
		}
	}

	public static void saveProperties()
	{
		try
		{
			FileOutputStream out = new FileOutputStream(propFile);
			properties.store(out, "TES4Gecko Properties");
			out.close();
		}
		catch (Exception exc)
		{
			logException("Exception while saving application properties", exc);
		}
	}

	public static void logException(String text, Throwable exc)
	{
		System.runFinalization();
		System.gc();

		if (SwingUtilities.isEventDispatchThread())
		{
			StringBuilder string = new StringBuilder(512);

			string.append("<html><b>");
			string.append(text);
			string.append("</b><br><br>");

			string.append(exc.toString());
			string.append("<br>");

			StackTraceElement[] trace = exc.getStackTrace();
			int count = 0;
			for (StackTraceElement elem : trace)
			{
				string.append(elem.toString());
				string.append("<br>");
				count++;
				if (count == 25)
				{
					break;
				}
			}
			string.append("</html>");
			JOptionPane.showMessageDialog(mainWindow, string, "Error", 0);
		}
		else if (deferredException == null)
		{
			deferredText = text;
			deferredException = exc;
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						Main.logException(Main.deferredText, Main.deferredException);
						Main.deferredException = null;
						Main.deferredText = null;
					}
				});
			}
			catch (Throwable swingException)
			{
				deferredException = null;
				deferredText = null;
			}
		}
	}

	public static void dumpData(String text, byte[] data, int length)
	{
		System.out.println(text);

		for (int i = 0; i < length; i++)
		{
			if (i % 32 == 0)
				System.out.print(String.format(" %14X  ", new Object[]
				{ Integer.valueOf(i) }));
			else if (i % 4 == 0)
			{
				System.out.print(" ");
			}
			System.out.print(String.format("%02X", new Object[]
			{ Byte.valueOf(data[i]) }));

			if (i % 32 == 31)
			{
				System.out.println();
			}
		}
		if (length % 32 != 0)
			System.out.println();
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.Main
 * JD-Core Version:    0.6.0
 */