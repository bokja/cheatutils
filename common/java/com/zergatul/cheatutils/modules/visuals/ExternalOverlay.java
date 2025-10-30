package com.zergatul.cheatutils.modules.visuals;
import com.zergatul.cheatutils.common.events.RenderWorldLastEvent;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ExternalOverlay {

    public static final ExternalOverlay instance = new ExternalOverlay();

    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicReference<ExternalOverlayBackend> backendRef = new AtomicReference<>();

    private ExternalOverlay() {
        // no-op
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void toggle() {
        setEnabled(!isEnabled());
    }

    public void setEnabled(boolean value) {
        boolean prev = enabled.getAndSet(value);
        ExternalOverlayBackend backend = backendRef.get();
        if (backend != null && prev != value) {
            if (value) {
                backend.onEnabled();
            } else {
                backend.onDisabled();
            }
        }
    }

    public void setBackend(ExternalOverlayBackend backend) {
        ExternalOverlayBackend prev = backendRef.getAndSet(backend);
        if (prev != null && prev != backend && isEnabled()) {
            prev.onDisabled();
        }
        if (backend != null && isEnabled()) {
            backend.onEnabled();
        }
    }

    public void submitBlockOverlays(RenderWorldLastEvent event, List<BlockPos> blocks, int rgba) {
        ExternalOverlayBackend backend = backendRef.get();
        if (backend != null) {
            backend.submitBlockOverlays(event, blocks, rgba);
        }
    }
}


