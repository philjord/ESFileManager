package esmio.common.data.plugin;

public class FormInfo {
	private String			recordType;

	private int				formID;

	private PluginRecord	pluginRecord;

	private boolean			pointerOnly	= false;

	private long			pointer;

	public FormInfo(String recordType, int formID, PluginRecord pluginRecord) {
		this.recordType = recordType;
		this.formID = formID;
		this.pluginRecord = pluginRecord;
	}

	public FormInfo(String recordType, int formID, long pointer) {
		this.recordType = recordType;
		this.formID = formID;
		this.pointer = pointer;
		this.pointerOnly = true;
	}

	public PluginRecord getPluginRecord() {
		return pluginRecord;
	}

	public long getPointer() {
		return pointer;
	}

	public String getRecordType() {
		return recordType;
	}

	public int getFormID() {
		return formID;
	}

	public boolean isPointerOnly() {
		return pointerOnly;
	}

	@Override
	public String toString() {
		return "recordType:" + recordType + " formID:" + formID + " pointer:" + pointer + " pointerOnly:" + pointerOnly;
	}

}
