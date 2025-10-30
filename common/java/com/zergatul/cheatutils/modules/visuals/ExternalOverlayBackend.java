package com.zergatul.cheatutils.modules.visuals;

import com.zergatul.cheatutils.common.events.RenderWorldLastEvent;
import net.minecraft.core.BlockPos;

import java.util.List;

public interface ExternalOverlayBackend {

    void onEnabled();

    void onDisabled();

    void submitBlockOverlays(RenderWorldLastEvent event, List<BlockPos> blocks, int rgba);
}


