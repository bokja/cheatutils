package com.zergatul.cheatutils.scripting.modules;

import com.zergatul.cheatutils.modules.visuals.ExternalOverlay;
import com.zergatul.cheatutils.scripting.ApiType;
import com.zergatul.cheatutils.scripting.ApiVisibility;
import com.zergatul.scripting.MethodDescription;

@SuppressWarnings("unused")
public class ExternalOverlayApi {

    @MethodDescription("Checks if external overlay is enabled")
    public boolean isEnabled() {
        return ExternalOverlay.instance.isEnabled();
    }

    @MethodDescription("Toggles external overlay on/off")
    @ApiVisibility(ApiType.UPDATE)
    public void toggle() {
        ExternalOverlay.instance.toggle();
    }

    @MethodDescription("Enables external overlay")
    @ApiVisibility(ApiType.UPDATE)
    public void enable() {
        ExternalOverlay.instance.setEnabled(true);
    }

    @MethodDescription("Disables external overlay")
    @ApiVisibility(ApiType.UPDATE)
    public void disable() {
        ExternalOverlay.instance.setEnabled(false);
    }
}


