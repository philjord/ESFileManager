package ca.davidfuchs.jessedit.ess;

import java.util.ArrayList;
import java.util.List;

public class StructPluginInfo {
    private short pluginCount;
    private List<String> plugins = new ArrayList<String>();

    public short getPluginCount() {
        return pluginCount;
    }

    void setPluginCount(short pluginCount) {
        this.pluginCount = pluginCount;
    }

    public List<String> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<String> plugins) {
        this.plugins = plugins;
    }

    @Override
    public String toString() {
        return "StructPluginInfo{" +
                "pluginCount=" + pluginCount +
                ", plugins=" + plugins +
                '}';
    }
}
