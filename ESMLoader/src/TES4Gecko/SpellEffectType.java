package TES4Gecko;

import java.util.HashMap;
import java.util.Map;

public final class SpellEffectType
{
	private static Map<String, String> spellEffectNameMap = new HashMap<String, String>();

	static
	{
		spellEffectNameMap.put("ABAT", "Absorb Attribute");
		spellEffectNameMap.put("ABFT", "Absorb Fatigue");
		spellEffectNameMap.put("ABHE", "Absorb Health");
		spellEffectNameMap.put("ABSK", "Absorb Skill");
		spellEffectNameMap.put("ABSP", "Absorb Magicka");
		spellEffectNameMap.put("BABO", "Bound Boots");
		spellEffectNameMap.put("BACU", "Bound Cuirass");
		spellEffectNameMap.put("BAGA", "Bound Gauntlets");
		spellEffectNameMap.put("BAGR", "Bound Greaves");
		spellEffectNameMap.put("BAHE", "Bound Helmet");
		spellEffectNameMap.put("BASH", "Bound Shield");
		spellEffectNameMap.put("BRDN", "Burden");
		spellEffectNameMap.put("BWAX", "Bound Axe");
		spellEffectNameMap.put("BWBO", "Bound Bow");
		spellEffectNameMap.put("BWDA", "Bound Dagger");
		spellEffectNameMap.put("BWMA", "Bound Mace");
		spellEffectNameMap.put("BWSW", "Bound Sword");
		spellEffectNameMap.put("CALM", "Calm");
		spellEffectNameMap.put("CHML", "Chameleon");
		spellEffectNameMap.put("CHRM", "Charm");
		spellEffectNameMap.put("COCR", "Command Creature");
		spellEffectNameMap.put("COHU", "Command Humanoid");
		spellEffectNameMap.put("CUDI", "Cure Disease");
		spellEffectNameMap.put("CUPA", "Cure Paralysis");
		spellEffectNameMap.put("CUPO", "Cure Poison");
		spellEffectNameMap.put("DARK", "Darkness");
		spellEffectNameMap.put("DEMO", "Demoralize");
		spellEffectNameMap.put("DGAT", "Damage Attribute");
		spellEffectNameMap.put("DGFA", "Damage Fatigue");
		spellEffectNameMap.put("DGHE", "Damage Health");
		spellEffectNameMap.put("DGSP", "Damage Magicka");
		spellEffectNameMap.put("DIAR", "Disintegrate Armor");
		spellEffectNameMap.put("DISE", "Disease Info");
		spellEffectNameMap.put("DIWE", "Disintegrate Weapon");
		spellEffectNameMap.put("DRAT", "Drain Attribute");
		spellEffectNameMap.put("DRFA", "Drain Fatigue");
		spellEffectNameMap.put("DRHE", "Drain Health");
		spellEffectNameMap.put("DRSK", "Drain Skill");
		spellEffectNameMap.put("DRSP", "Drain Magicka");
		spellEffectNameMap.put("DSPL", "Dispel");
		spellEffectNameMap.put("DTCT", "Detect Life");
		spellEffectNameMap.put("DUMY", "Mehrunes Dagon Custom Effect");
		spellEffectNameMap.put("FIDG", "Fire Damage");
		spellEffectNameMap.put("FISH", "Fire Shield");
		spellEffectNameMap.put("FOAT", "Fortify Attribute");
		spellEffectNameMap.put("FOFA", "Fortify Fatigue");
		spellEffectNameMap.put("FOHE", "Fortify Health");
		spellEffectNameMap.put("FOSK", "Fortify Skill");
		spellEffectNameMap.put("FOSP", "Fortify Magicka");
		spellEffectNameMap.put("FRDG", "Frost Damage");
		spellEffectNameMap.put("FRNZ", "Frenzy");
		spellEffectNameMap.put("FRSH", "Frost Shield");
		spellEffectNameMap.put("FTHR", "Feather");
		spellEffectNameMap.put("INVI", "Invisibility");
		spellEffectNameMap.put("LGHT", "Light");
		spellEffectNameMap.put("LISH", "Shock Shield");
		spellEffectNameMap.put("LOCK", "Lock");
		spellEffectNameMap.put("MYHL", "Summon Mythic Dawn Helmet");
		spellEffectNameMap.put("MYTH", "Summon Mythic Dawn Armor");
		spellEffectNameMap.put("NEYE", "Night-Eye");
		spellEffectNameMap.put("OPEN", "Open");
		spellEffectNameMap.put("PARA", "Paralyze");
		spellEffectNameMap.put("POSN", "Poison Info");
		spellEffectNameMap.put("RALY", "Rally");
		spellEffectNameMap.put("REAN", "Reanimate");
		spellEffectNameMap.put("REAT", "Restore Attribute");
		spellEffectNameMap.put("REFA", "Restore Fatigue");
		spellEffectNameMap.put("REHE", "Restore Health");
		spellEffectNameMap.put("RESP", "Restore Magicka");
		spellEffectNameMap.put("RFDG", "Reflect Damage");
		spellEffectNameMap.put("RFLC", "Reflect Spell");
		spellEffectNameMap.put("RSDI", "Resist Disease");
		spellEffectNameMap.put("RSFI", "Resist Fire");
		spellEffectNameMap.put("RSFR", "Resist Frost");
		spellEffectNameMap.put("RSMA", "Resist Magic");
		spellEffectNameMap.put("RSNW", "Resist Normal Weapons");
		spellEffectNameMap.put("RSPA", "Resist Paralysis");
		spellEffectNameMap.put("RSPO", "Resist Poison");
		spellEffectNameMap.put("RSSH", "Resist Shock");
		spellEffectNameMap.put("RSWD", "Resist Water Damage");
		spellEffectNameMap.put("SABS", "Spell Absorption");
		spellEffectNameMap.put("SEFF", "Script Effect");
		spellEffectNameMap.put("SHDG", "Shock Damage");
		spellEffectNameMap.put("SHLD", "Shield");
		spellEffectNameMap.put("SLNC", "Silence");
		spellEffectNameMap.put("STMA", "Stunted Magicka");
		spellEffectNameMap.put("STRP", "Soul Trap");
		spellEffectNameMap.put("SUDG", "Sun Damage");
		spellEffectNameMap.put("TELE", "Telekinesis");
		spellEffectNameMap.put("TURN", "Turn Undead");
		spellEffectNameMap.put("VAMP", "Vampirism");
		spellEffectNameMap.put("WABR", "Water Breathing");
		spellEffectNameMap.put("WAWA", "Water Walking");
		spellEffectNameMap.put("WKDI", "Weakness to Disease");
		spellEffectNameMap.put("WKFI", "Weakness to Fire");
		spellEffectNameMap.put("WKFR", "Weakness to Frost");
		spellEffectNameMap.put("WKMA", "Weakness to Magic");
		spellEffectNameMap.put("WKNW", "Weakness to Normal Weapons");
		spellEffectNameMap.put("WKPO", "Weakness to Poison");
		spellEffectNameMap.put("WKSH", "Weakness to Shock");
		spellEffectNameMap.put("ZCLA", "Summon Clannfear");
		spellEffectNameMap.put("ZDAE", "Summon Daedroth");
		spellEffectNameMap.put("ZDRE", "Summon Dremora");
		spellEffectNameMap.put("ZDRL", "Summon Dremora Lord");
		spellEffectNameMap.put("ZFIA", "Summon Flame Atronach");
		spellEffectNameMap.put("ZFRA", "Summon Frost Atronach");
		spellEffectNameMap.put("ZGHO", "Summon Ghost");
		spellEffectNameMap.put("ZHDZ", "Summon Headless Zombie");
		spellEffectNameMap.put("ZLIC", "Summon Lich");
		spellEffectNameMap.put("ZSCA", "Summon Scamp");
		spellEffectNameMap.put("ZSKA", "Summon Skeleton Guardian");
		spellEffectNameMap.put("ZSKC", "Summon Skeleton Champion");
		spellEffectNameMap.put("ZSKE", "Summon Skeleton");
		spellEffectNameMap.put("ZSKH", "Summon Skeleton Hero");
		spellEffectNameMap.put("ZSPD", "Summon Spider Daedra");
		spellEffectNameMap.put("ZSTA", "Summon Storm Atronach");
		spellEffectNameMap.put("ZWRA", "Summon Faded Wraith");
		spellEffectNameMap.put("ZWRL", "Summon Gloom Wraith");
		spellEffectNameMap.put("ZXIV", "Summon Xivilai");
		spellEffectNameMap.put("ZZOM", "Summon Zombie");
	}

	public static String getSpellEffectName(String spellEffectID)
	{
		if (!spellEffectNameMap.containsKey(spellEffectID))
			return "Unknown Spell Effect [" + spellEffectID + "]";
		return spellEffectNameMap.get(spellEffectID);
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.SpellEffectType
 * JD-Core Version:    0.6.0
 */