/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.reports.web;

import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.web.gui.components.WebComponentsHelper;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.Component;

/**
 */
public final class WindowCompanionHelper {
    private WindowCompanionHelper() {
    }

    public static void setWindowWidth(Window window, int width) {
        if (window != null) {
            Component component = WebComponentsHelper.unwrap(window);
            while ( !(component.getParent() instanceof com.vaadin.ui.Window || component.getParent() == null)) {
                component = component.getParent();
            }

            if (component.getParent() != null) {
                com.vaadin.ui.Window vaadinWindow = (com.vaadin.ui.Window) component.getParent();
                vaadinWindow.setWidth(width, Sizeable.Unit.PIXELS);
                vaadinWindow.setWindowMode(WindowMode.NORMAL);
                vaadinWindow.center();
            }
        }
    }

    public static void setWindowHeight(Window window, int height) {
        if (window != null) {
            Component component = WebComponentsHelper.unwrap(window);
            while ( !(component.getParent() instanceof com.vaadin.ui.Window || component.getParent() == null)) {
                component = component.getParent();
            }

            if (component.getParent() != null) {
                com.vaadin.ui.Window vaadinWindow = (com.vaadin.ui.Window) component.getParent();
                vaadinWindow.setHeight(height, Sizeable.Unit.PIXELS);
                vaadinWindow.setWindowMode(WindowMode.NORMAL);
                vaadinWindow.center();
            }
        }
    }

    public static void center(Window window) {
        if (window != null) {
            Component component = WebComponentsHelper.unwrap(window);
            while ( !(component.getParent() instanceof com.vaadin.ui.Window || component.getParent() == null)) {
                component = component.getParent();
            }

            if (component.getParent() != null) {
                com.vaadin.ui.Window vaadinWindow = (com.vaadin.ui.Window) component.getParent();
                vaadinWindow.center();
            }
        }
    }

    public static void setWindowResizable(Window window, boolean resizable) {
        if (window != null) {
            Component component = WebComponentsHelper.unwrap(window);
            while ( !(component.getParent() instanceof com.vaadin.ui.Window || component.getParent() == null)) {
                component = component.getParent();
            }

            if (component.getParent() != null) {
                com.vaadin.ui.Window vaadinWindow = (com.vaadin.ui.Window) component.getParent();
                vaadinWindow.setResizable(resizable);
            }
        }
    }
}
