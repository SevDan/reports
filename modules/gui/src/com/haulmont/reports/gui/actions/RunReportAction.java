/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.reports.gui.actions;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.WindowManager.OpenType;
import com.haulmont.cuba.gui.components.AbstractAction;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Frame;
import com.haulmont.reports.entity.Report;
import com.haulmont.reports.gui.ReportGuiManager;
import com.haulmont.reports.gui.report.run.ReportRun;

import java.util.Collections;

import static com.google.common.base.Preconditions.checkArgument;

/**
 */
public class RunReportAction extends AbstractAction implements Action.HasBeforeAfterHandlers {

    protected final Frame window;

    protected Messages messages = AppBeans.get(Messages.class);

    protected ReportGuiManager reportGuiManager = AppBeans.get(ReportGuiManager.class);

    protected Runnable beforeActionPerformedHandler;
    protected Runnable afterActionPerformedHandler;

    public RunReportAction(Frame window) {
        this("runReport", window);
    }

    public RunReportAction(String id, Frame window) {
        super(id);

        checkArgument(window != null, "Can not create RunReportAction with null window");

        this.window = window;
        this.caption = messages.getMessage(getClass(), "actions.Report");
        this.icon = "icons/reports-print.png";
    }

    @Override
    public void actionPerform(Component component) {
        if (beforeActionPerformedHandler != null) {
            beforeActionPerformedHandler.run();
        }

        window.openLookup("report$Report.run", items -> {
            if (items != null && items.size() > 0) {
                Report report = (Report) items.iterator().next();
                report = window.getDsContext().getDataSupplier().reload(report, "report.edit");
                if (report.getInputParameters() != null && report.getInputParameters().size() > 0) {
                    openReportParamsDialog(report, window);
                } else {
                    reportGuiManager.printReport(report, Collections.<String, Object>emptyMap(), window);
                }
            }
        }, OpenType.DIALOG, ParamsMap.of(ReportRun.SCREEN_PARAMETER, window.getId()));

        if (afterActionPerformedHandler != null) {
            afterActionPerformedHandler.run();
        }
    }

    protected void openReportParamsDialog(Report report, Frame window) {
        window.openWindow("report$inputParameters", OpenType.DIALOG, ParamsMap.of("report", report));
    }

    @Override
    public Runnable getBeforeActionPerformedHandler() {
        return beforeActionPerformedHandler;
    }

    @Override
    public void setBeforeActionPerformedHandler(Runnable handler) {
        this.beforeActionPerformedHandler = handler;
    }

    @Override
    public Runnable getAfterActionPerformedHandler() {
        return afterActionPerformedHandler;
    }

    @Override
    public void setAfterActionPerformedHandler(Runnable handler) {
        this.afterActionPerformedHandler = handler;
    }
}