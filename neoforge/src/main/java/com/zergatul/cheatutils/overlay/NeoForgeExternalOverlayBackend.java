package com.zergatul.cheatutils.overlay;

import com.zergatul.cheatutils.common.events.RenderWorldLastEvent;
import com.zergatul.cheatutils.modules.visuals.ExternalOverlayBackend;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import javax.swing.*;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

public class NeoForgeExternalOverlayBackend implements ExternalOverlayBackend {

    private final Minecraft mc = Minecraft.getInstance();

    private volatile Win32OverlayWindow window;
    private volatile long mcGlfwWindow;
    // no need to cache HWND; we align via GLFW APIs

    @Override
    public void onEnabled() {
        if (!isWindows()) {
            return;
        }
        ensureWindow();
    }

    @Override
    public void onDisabled() {
        if (window != null) {
            SwingUtilities.invokeLater(() -> {
                window.setVisible(false);
                window.dispose();
            });
            window = null;
        }
    }

    @Override
    public void submitBlockOverlays(RenderWorldLastEvent event, List<BlockPos> blocks, int rgba) {
        if (!isWindows() || blocks == null || blocks.isEmpty()) {
            return;
        }
        ensureWindow();
        if (window == null) {
            return;
        }

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        int halfW = width / 2;
        int halfH = height / 2;

        Matrix4f pose = event.getPose();
        Matrix4f proj = event.getProjection();

        double camX = event.getCamera().getPosition().x;
        double camY = event.getCamera().getPosition().y;
        double camZ = event.getCamera().getPosition().z;

        List<Shape> shapes = new ArrayList<>();
        for (BlockPos pos : blocks) {
            float x1 = (float) (pos.getX() - camX);
            float y1 = (float) (pos.getY() - camY);
            float z1 = (float) (pos.getZ() - camZ);
            float x2 = x1 + 1f;
            float y2 = y1 + 1f;
            float z2 = z1 + 1f;

            // 8 corners
            float[][] corners = new float[][]{
                    {x1, y1, z1}, {x2, y1, z1}, {x1, y2, z1}, {x2, y2, z1},
                    {x1, y1, z2}, {x2, y1, z2}, {x1, y2, z2}, {x2, y2, z2}
            };

            boolean anyInFront = false;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

            for (float[] c : corners) {
                Vector4f v = new Vector4f(c[0], c[1], c[2], 1f);
                Vector4f v1 = pose.transform(new Vector4f(v));
                Vector4f v2 = proj.transform(v1);
                if (v2.z > 0f) {
                    anyInFront = true;
                    int sx = Math.round(v2.x / v2.w * halfW + halfW);
                    int sy = Math.round(-v2.y / v2.w * halfH + halfH);
                    if (sx < minX) minX = sx;
                    if (sy < minY) minY = sy;
                    if (sx > maxX) maxX = sx;
                    if (sy > maxY) maxY = sy;
                }
            }

            if (anyInFront && minX < maxX && minY < maxY) {
                shapes.add(new Rectangle(minX, minY, maxX - minX, maxY - minY));
            }
        }

        alignOverlayToMinecraft();
        Color color = new Color(rgba, true);
        SwingUtilities.invokeLater(() -> {
            if (window != null) {
                window.setVisible(true);
                window.setFrame(shapes, color);
            }
        });
    }

    private void ensureWindow() {
        if (window != null) {
            return;
        }
        if (!isWindows()) {
            return;
        }
        long ctx = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
        mcGlfwWindow = ctx != 0 ? ctx : mcGlfwWindow;
        SwingUtilities.invokeLater(() -> {
            if (window == null) {
                window = new Win32OverlayWindow();
                window.pack();
                window.enableClickThrough();
            }
        });
    }

    private void alignOverlayToMinecraft() {
        if (window == null || mcGlfwWindow == 0) {
            return;
        }
        int[] wx = new int[1];
        int[] wy = new int[1];
        org.lwjgl.glfw.GLFW.glfwGetWindowPos(mcGlfwWindow, wx, wy);
        int[] left = new int[1];
        int[] top = new int[1];
        int[] right = new int[1];
        int[] bottom = new int[1];
        org.lwjgl.glfw.GLFW.glfwGetWindowFrameSize(mcGlfwWindow, left, top, right, bottom);
        int clientX = wx[0] + left[0];
        int clientY = wy[0] + top[0];
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        SwingUtilities.invokeLater(() -> {
            if (window != null) {
                window.setBounds(clientX, clientY, width, height);
            }
        });
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase().contains("win");
    }
}


