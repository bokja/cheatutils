package com.zergatul.cheatutils.overlay;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Win32OverlayWindow extends JWindow {

    private final List<Shape> shapes = new CopyOnWriteArrayList<>();
    private volatile Color color = new Color(255, 0, 0, 64);

    public Win32OverlayWindow() {
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0));
        setFocusableWindowState(false);
        setFocusable(false);
        setIgnoreRepaint(false);
    }

    public void enableClickThrough() {
        WinDef.HWND hwnd = new WinDef.HWND(Native.getComponentPointer(this));
        int exStyle = User32.INSTANCE.GetWindowLong(hwnd, -20 /* GWL_EXSTYLE */);
        exStyle |= 0x00080000 /* WS_EX_LAYERED */ | 0x00000020 /* WS_EX_TRANSPARENT */ | 0x00000080 /* WS_EX_TOOLWINDOW */;
        User32.INSTANCE.SetWindowLong(hwnd, -20 /* GWL_EXSTYLE */, exStyle);
    }

    public void setFrame(List<Shape> newShapes, Color c) {
        shapes.clear();
        if (newShapes != null) {
            shapes.addAll(newShapes);
        }
        if (c != null) {
            color = c;
        }
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        for (Shape s : shapes) {
            g2.fill(s);
        }
        g2.dispose();
    }
}


