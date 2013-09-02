package esmLoader;

import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;

public class EsmFileLocations
{
	public static boolean ESM_MAKE_J3D_POINTLIGHTS = false;

	private static String GENERAL_ESM_FILE = null;

	private static String OBLIVION_ESM_FILE = null;

	private static String FALLOUT3_ESM_FILE = null;

	private static String FALLOUTNV_ESM_FILE = null;

	private static String SKYRIM_ESM_FILE = null;

	private static Preferences prefs;
	static
	{
		prefs = Preferences.userNodeForPackage(EsmFileLocations.class);
	}

	public static String getGeneralEsmFile()
	{
		if (GENERAL_ESM_FILE == null)
		{
			File f = requestEsmFileName("Select ESM File", prefs.get("General", ""));
			if (f == null)
			{
				return null;
			}
			else
			{
				prefs.put("General", f.getAbsolutePath());
				GENERAL_ESM_FILE = f.getAbsolutePath();
			}
		}

		return GENERAL_ESM_FILE;
	}

	public static String getOblivionEsmFile()
	{
		if (OBLIVION_ESM_FILE == null)
		{
			File f = requestEsmFileName("Select Oblivion ESM File", prefs.get("Oblivion", ""));
			if (f == null)
			{
				return null;
			}
			else
			{
				prefs.put("Oblivion", f.getAbsolutePath());
				OBLIVION_ESM_FILE = f.getAbsolutePath();
			}
		}

		return OBLIVION_ESM_FILE;
	}

	public static String getFallout3EsmFile()
	{
		if (FALLOUT3_ESM_FILE == null)
		{
			File f = requestEsmFileName("Select Fallout3 ESM File", prefs.get("Fallout3", ""));
			if (f == null)
			{
				return null;
			}
			else
			{
				prefs.put("Fallout3", f.getAbsolutePath());
				FALLOUT3_ESM_FILE = f.getAbsolutePath();
			}
		}

		return FALLOUT3_ESM_FILE;
	}

	public static String getFalloutNVEsmFile()
	{
		if (FALLOUTNV_ESM_FILE == null)
		{
			File f = requestEsmFileName("Select FalloutNV ESM File", prefs.get("FalloutNV", ""));
			if (f == null)
			{
				return null;
			}
			else
			{
				prefs.put("FalloutNV", f.getAbsolutePath());
				FALLOUTNV_ESM_FILE = f.getAbsolutePath();
			}
		}

		return FALLOUTNV_ESM_FILE;
	}

	public static String getSkyrimEsmFile()
	{
		if (SKYRIM_ESM_FILE == null)
		{
			File f = requestEsmFileName("Select Skyrim ESM File", prefs.get("Skyrim", ""));
			if (f == null)
			{
				return null;
			}
			else
			{
				prefs.put("Skyrim", f.getAbsolutePath());
				SKYRIM_ESM_FILE = f.getAbsolutePath();
			}
		}

		return SKYRIM_ESM_FILE;
	}

	private static File requestEsmFileName(String title, String defaultFile)
	{
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setSelectedFile(new File(defaultFile));
		fc.setDialogTitle(title);
		fc.showOpenDialog(null);
		File sf = fc.getSelectedFile();
		return sf;
	}

}
