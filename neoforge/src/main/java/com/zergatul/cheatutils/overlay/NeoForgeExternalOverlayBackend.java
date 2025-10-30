package com.zergatul.cheatutils.overlay;

import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.common.events.RenderGuiEvent;
import com.zergatul.cheatutils.common.events.RenderWorldLastEvent;
import com.zergatul.cheatutils.modules.visuals.ExternalOverlayBackend;
import com.zergatul.cheatutils.render.gl.FrameBuffer;
import com.zergatul.cheatutils.render.FrameBuffers;
import com.zergatul.cheatutils.render.gl.OverlayDrawProgram;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;

import java.util.concurrent.atomic.AtomicBoolean;

public class NeoForgeExternalOverlayBackend implements ExternalOverlayBackend {

	private final Minecraft mc = Minecraft.getInstance();

	// GLFW overlay window
    private volatile long overlayWindow = 0;
    private volatile long mcGlfwWindow = 0;
	private final AtomicBoolean glInited = new AtomicBoolean(false);
	private OverlayDrawProgram presenter;
	private volatile boolean enabledFlag = false;

	public NeoForgeExternalOverlayBackend() {
		// hook frame lifecycle to clear and present
		Events.BeforeRenderWorld.add(this::onBeforeRenderWorld);
		Events.PostRenderGui.add(this::onPostRenderGui);
	}

	@Override
	public void onEnabled() {
		enabledFlag = true;
		ensureWindow();
	}

	@Override
	public void onDisabled() {
		enabledFlag = false;
		if (overlayWindow != 0) {
			GLFW.glfwHideWindow(overlayWindow);
		}
	}

	@Override
	public void submitBlockOverlays(RenderWorldLastEvent event, java.util.List<net.minecraft.core.BlockPos> blocks, int rgba) {
		// no-op; Block ESP already renders into FrameBuffers.get1() in external-overlay mode
	}

	private void onBeforeRenderWorld() {
		// clear overlay color once per frame
		if (!enabledFlag || overlayWindow == 0) {
			return;
		}
		FrameBuffer.push();
		FrameBuffers.get1().bind();
		GL30.glClearColor(0f, 0f, 0f, 0f);
		GL30.glClear(GL30.GL_COLOR_BUFFER_BIT);
		FrameBuffer.pop();
	}

	private void onPostRenderGui(RenderGuiEvent event) {
		present();
	}

	private void ensureWindow() {
		if (overlayWindow != 0) {
			return;
		}
		if (!isWindows()) {
			return;
		}
		// must run on render thread with a current context
        long current = GLFW.glfwGetCurrentContext();
        if (current == 0) {
			return;
		}
        mcGlfwWindow = mc.getWindow().handle();

		GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, GLFW.GLFW_TRUE);
		GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);

		int width = mc.getWindow().getWidth();
		int height = mc.getWindow().getHeight();
        overlayWindow = GLFW.glfwCreateWindow(width, height, "", 0, mcGlfwWindow);
		if (overlayWindow == 0) {
			return;
		}

		GLFW.glfwSetWindowAttrib(overlayWindow, GLFW.GLFW_FLOATING, GLFW.GLFW_TRUE);
		// mouse passthrough (GLFW 3.3+)
		final int GLFW_MOUSE_PASSTHROUGH = 0x0002000D;
		GLFW.glfwSetWindowAttrib(overlayWindow, GLFW_MOUSE_PASSTHROUGH, GLFW.GLFW_TRUE);

		// initialize GL for overlay context
        GLFW.glfwMakeContextCurrent(overlayWindow);
		if (glInited.compareAndSet(false, true)) {
			GL.createCapabilities();
			presenter = new OverlayDrawProgram();
		}
		GLFW.glfwSwapInterval(0);
		GLFW.glfwMakeContextCurrent(current);
	}

	private void present() {
		if (!enabledFlag || overlayWindow == 0) {
			return;
		}

		// mirror size/position to MC client area
        long mcWindow = mcGlfwWindow != 0 ? mcGlfwWindow : mc.getWindow().handle();
        int[] wx = new int[1];
        int[] wy = new int[1];
        GLFW.glfwGetWindowPos(mcWindow, wx, wy);
        int[] left = new int[1];
        int[] top = new int[1];
        int[] right = new int[1];
        int[] bottom = new int[1];
        GLFW.glfwGetWindowFrameSize(mcWindow, left, top, right, bottom);
        int clientX = wx[0] + left[0];
        int clientY = wy[0] + top[0];
        int widthPx = mc.getWindow().getWidth();
        int heightPx = mc.getWindow().getHeight();

        float[] scaleX = new float[1];
        float[] scaleY = new float[1];
        GLFW.glfwGetWindowContentScale(mcWindow, scaleX, scaleY);
        int widthWU = Math.max(1, Math.round(widthPx / Math.max(0.0001f, scaleX[0])));
        int heightWU = Math.max(1, Math.round(heightPx / Math.max(0.0001f, scaleY[0])));
        GLFW.glfwSetWindowPos(overlayWindow, clientX, clientY);
        GLFW.glfwSetWindowSize(overlayWindow, widthWU, heightWU);
		GLFW.glfwShowWindow(overlayWindow);

		// draw texture into overlay window backbuffer
        GLFW.glfwMakeContextCurrent(overlayWindow);
		int[] fbw = new int[1];
		int[] fbh = new int[1];
		GLFW.glfwGetFramebufferSize(overlayWindow, fbw, fbh);
		GL30.glViewport(0, 0, fbw[0], fbh[0]);

        // one-shot correction if framebuffer size doesn't match MC size (DPI rounding)
        if ((fbw[0] != widthPx || fbh[0] != heightPx) && widthWU > 0 && heightWU > 0) {
            int correctedW = Math.max(1, Math.round((float) widthWU * widthPx / Math.max(1, fbw[0])));
            int correctedH = Math.max(1, Math.round((float) heightWU * heightPx / Math.max(1, fbh[0])));
            if (correctedW != widthWU || correctedH != heightWU) {
                GLFW.glfwSetWindowSize(overlayWindow, correctedW, correctedH);
                GLFW.glfwGetFramebufferSize(overlayWindow, fbw, fbh);
                GL30.glViewport(0, 0, fbw[0], fbh[0]);
            }
        }
		GL30.glClearColor(0f, 0f, 0f, 0f);
		GL30.glClear(GL30.GL_COLOR_BUFFER_BIT);

        // build fullscreen quad (NDC) once per present (x, y, z, u, v)
        presenter.buffer.clear();
        presenter.buffer.add(-1); presenter.buffer.add(-1); presenter.buffer.add(0); presenter.buffer.add(0); presenter.buffer.add(0);
        presenter.buffer.add( 1); presenter.buffer.add(-1); presenter.buffer.add(0); presenter.buffer.add(1); presenter.buffer.add(0);
        presenter.buffer.add(-1); presenter.buffer.add( 1); presenter.buffer.add(0); presenter.buffer.add(0); presenter.buffer.add(1);
        presenter.buffer.add( 1); presenter.buffer.add( 1); presenter.buffer.add(0); presenter.buffer.add(1); presenter.buffer.add(1);
        presenter.buffer.add(-1); presenter.buffer.add( 1); presenter.buffer.add(0); presenter.buffer.add(0); presenter.buffer.add(1);
        presenter.buffer.add( 1); presenter.buffer.add(-1); presenter.buffer.add(0); presenter.buffer.add(1); presenter.buffer.add(0);

		presenter.draw(FrameBuffers.get1(), 1f, 1f, 1f, 1f);
		presenter.unbind();
        GLFW.glfwSwapBuffers(overlayWindow);
        GLFW.glfwMakeContextCurrent(mcWindow);
	}

	private static boolean isWindows() {
		String os = System.getProperty("os.name", "");
		return os.toLowerCase().contains("win");
	}
}


