package ca.davidfuchs.jessedit.ess;

import java.util.Arrays;

public class StructChangeForms {
    private StructRefId refId;
    private long changeFlags;
    private short type;
    private short version;
    private long length1;
    private long length2;
    private byte[] data;

    public StructRefId getRefId() {
        return refId;
    }

    void setRefId(StructRefId refId) {
        this.refId = refId;
    }

    public long getChangeFlags() {
        return changeFlags;
    }

    void setChangeFlags(long changeFlags) {
        this.changeFlags = changeFlags;
    }

    public short getType() {
        return type;
    }

    void setType(short type) {
        this.type = type;
    }

    // The upper two bits of the 'type' field represent the data type used to store the length of the data section.
    // 0 = unsigned byte, 1 = unsigned short, 2 = unsigned int
    public byte getFieldLengthType() {
        return (byte) ((type & 0b11000000) >> 6);
    }

    // The lower 6 bits of the 'type' field represent the type of change form.  We try to map this to an enumeration
    // that stores known change form types.  The enumeration is not exhaustive and might be missing a lot of types.
    public ChangeFormType getChangeFormType() {
        for (ChangeFormType changeFormType : ChangeFormType.values()) {
            if (changeFormType.ordinal() == (byte) (type & 0b00111111)) {
                return changeFormType;
            }
        }

        return ChangeFormType.UNKNOWN;
    }

    public short getVersion() {
        return version;
    }

    void setVersion(short version) {
        this.version = version;
    }

    public long getLength1() {
        return length1;
    }

    void setLength1(long length1) {
        this.length1 = length1;
    }

    public long getLength2() {
        return length2;
    }

    void setLength2(long length2) {
        this.length2 = length2;
    }

    public byte[] getData() {
        return data;
    }

    void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "StructChangeForms{" +
                "refId=" + refId +
                ", changeFlags=" + changeFlags +
                ", type=" + type +
                ", changeFormType=" + getChangeFormType() +
                ", version=" + version +
                ", length1=" + length1 +
                ", length2=" + length2 +
                ", data=" + Arrays.toString(data) +
                '}';
    }

    // From what I can gather, I believe Skyrim uses a list of key/value pairs to store all it's data.
    // When you load a game, it takes all the default values, and applies these change forms to them, to build up
    // the environment according to the save game.  This list of change form types is not exhaustive, I just
    // found them online at http://www.uesp.net/wiki/Tes5Mod:Save_File_Format
    public enum ChangeFormType {
        REFR((byte) 63, "ObjectReference"), ACHR((byte) 64, "Actor"), PMIS((byte) 65), PGRE((byte) 67), PBEA((byte) 68), PFLA((byte) 69),
        CELL((byte) 62), INFO((byte) 78, "TopicInfo"), QUST((byte) 79, "Quest"), NPC_((byte) 45, "ActorBase"), ACTI((byte) 25, "Activator"), TACT((byte) 26, "TalkingActivator"),
        ARMO((byte) 27, "Armor"), BOOK((byte) 28, "Book"), CONT((byte) 29, "Container"), DOOR((byte) 30, "Door"), INGR((byte) 31, "Ingredient"), LIGH((byte) 32, "Light"),
        MISC((byte) 33, "MiscObject"), APPA((byte) 34, "Apparatus"), STAT((byte) 35, "Static"), MSTT((byte) 37), FURN((byte) 42, "Furniture"), WEAP((byte) 43, "Weapon"),
        AMMO((byte) 44, "Ammo"), KEYM((byte) 47, "Key"), ALCH((byte) 48, "Potion"), IDLM((byte) 49), NOTE((byte) 50), ECZN((byte) 105, "EncounterZone"),
        CLAS((byte) 10, "Class"), FACT((byte) 11, "Faction"), PACK((byte) 81, "Package"), NAVM((byte) 75), WOOP((byte) 120, "WordOfPower"), MGEF((byte) 19, "MagicEffect"),
        SMQN((byte) 115), SCEN((byte) 124, "Scene"), LCTN((byte) 106, "Location"), RELA((byte) 123), PHZD((byte) 72), PBAR((byte) 71),
        PCON((byte) 70), FLST((byte) 93, "FormList"), LVLN((byte) 46, "LeveledActor"), LVLI((byte) 55, "LeveledItem"), LVSP((byte) 84, "LeveledSpell"), PARW((byte) 66),
        ENCH((byte) 22, "Enchantment"), UNKNOWN((byte) -1);

        private byte formType;
        private String formName;

        ChangeFormType(byte formType) {
            this.formType = formType;
            this.formName = "Unknown";
        }

        ChangeFormType(byte formType, String formName) {
            this.formType = formType;
            this.formName = formName;
        }

        public byte getFormType() {
            return formType;
        }

        public String getFormName() {
            return formName;
        }
    }
}

