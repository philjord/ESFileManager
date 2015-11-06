package utils.source;

import java.util.HashMap;

import esmmanager.common.data.record.Record;
import esmmanager.common.data.record.Subrecord;
import esmmanager.loader.IESMManager;

public class EsmSoundKeyToName implements SoundKeyToName
{
	public static HashMap<String, String> soundNameToFile = new HashMap<String, String>();

	public EsmSoundKeyToName(IESMManager esmManager)
	{
		//TODO: I very much to to traverse only the SOUN GRUP records here
		for (Integer formId : esmManager.getTypeToFormIdMap().get("SOUN"))
		{
			Record rec = esmManager.getRecord(formId);
			if (rec.getRecordType().equals("SOUN"))
			{
				String key = "";
				String value = "";
				for (Subrecord sub : rec.getSubrecords())
				{
					byte[] bytes = sub.getData();
					if (sub.getType().equals("EDID"))
					{
						key = new String(bytes, 0, bytes.length - 1);
					}
					else if (sub.getType().equals("FNAM"))// If not exact name presumably a random sound is selected, dictated by SNDX data
					{
						value = new String(bytes, 0, bytes.length - 1);
					}
					else if (sub.getType().equals("SNDX"))//12 bytes
					{
						// do nothing, sexy sound info see SNDX in esmj3dfo3 subrecords
					}
					else if (sub.getType().equals("SNDD"))//36 bytes
					{
						// no idea
					}
					else if (sub.getType().equals("OBND"))//12 bytes
					{
						// bounds info
					}
					else if (sub.getType().equals("ANAM"))//10 bytes
					{

					}
					else if (sub.getType().equals("GNAM"))// 2 bytes
					{

					}
					else if (sub.getType().equals("HNAM"))// 4 bytes
					{

					}

				}
				if (key.length() > 0)
				{
					soundNameToFile.put(key, value);
				}
			}
		}
	}

	//TODO: a system for picking sounds from folder names, 
	//but that requires soundsources, which I think is circular
	public String getFileName(String soundName)
	{

		String ret = soundNameToFile.get(soundName);

		if (ret == null)
		{
			//sometimes it's the file itself not the key
			if (soundNameToFile.containsValue(soundName))
				ret = soundName;
		}

		if (ret != null)
		{
			return "sound\\" + ret;
			/*File snf = new File(ret);
			if (snf.exists())
			{
				if (!snf.isDirectory())
				{
					return ret;
				}
				else
				{
					//TODO: folder based random sound
					return ret;
				}
			}*/
		}

		System.out.println("Sound File Not LookedUp!!! " + soundName);

		return null;
	}
}
