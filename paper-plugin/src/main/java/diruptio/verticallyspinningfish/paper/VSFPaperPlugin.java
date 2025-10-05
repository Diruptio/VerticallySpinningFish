package diruptio.verticallyspinningfish.paper;

import diruptio.verticallyspinningfish.VerticallySpinningFishApi;
import diruptio.verticallyspinningfish.api.Status;
import org.bukkit.plugin.java.JavaPlugin;

public class VSFPaperPlugin extends JavaPlugin {
    private VerticallySpinningFishApi api;

    @Override
    public void onEnable() {
        api = VerticallySpinningFishApi.fromCurrentContainer();
        api.setContainerStatus(api.getCurrentContainerId(), Status.AVAILABLE);
    }

    @Override
    public void onDisable() {
        api.close();
    }
}
