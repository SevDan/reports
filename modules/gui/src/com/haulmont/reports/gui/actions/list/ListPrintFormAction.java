/*
 * Copyright (c) 2008-2019 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.reports.gui.actions.list;

import com.google.common.base.Preconditions;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.data.BindingState;
import com.haulmont.cuba.gui.components.data.meta.ContainerDataUnit;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.meta.StudioAction;
import com.haulmont.cuba.gui.model.*;
import com.haulmont.cuba.gui.screen.ScreenContext;
import com.haulmont.reports.app.ParameterPrototype;
import com.haulmont.reports.gui.ReportGuiManager;
import com.haulmont.reports.gui.actions.AbstractPrintFormAction;
import org.apache.commons.collections4.CollectionUtils;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

/**
 * Standard action for printing reports for a list of entity instances associated with a list component.
 * <p>
 * Should be defined for a list component ({@code Table}, {@code DataGrid}, etc.) in a screen XML descriptor.
 * <p>
 * The action only selects reports having an external parameter of the {@code Entity} or the List of entities type
 * and where the parameter entity type matches the entity type displayed by the list component.
 * If only one report is available as a result of selection, it is invoked immediately.
 * If several reports are available, their list is offered to the user for selection.
 */
@StudioAction(category = "Reports list actions", description = "Prints the reports for a list of entity instances associated with a list component")
@ActionType(ListPrintFormAction.ID)
public class ListPrintFormAction extends AbstractPrintFormAction implements Action.HasTarget {

    public static final String ID = "listPrintForm";

    protected Messages messages;

    protected ListComponent target;

    public ListPrintFormAction() {
        this(ID);
    }

    public ListPrintFormAction(String id) {
        super(id);
    }

    @Inject
    public void setIcons(Icons icons) {
        this.icon = icons.get(CubaIcon.PRINT);
    }

    @Inject
    public void setMessages(Messages messages) {
        this.messages = messages;
        this.caption = messages.getMessage(ListPrintFormAction.class, "actions.ListPrintForm");
    }

    @Override
    public ListComponent getTarget() {
        return target;
    }

    @Override
    public void setTarget(ListComponent target) {
        this.target = target;
    }

    @Override
    public void actionPerform(Component component) {
        DialogAction cancelAction = new DialogAction(DialogAction.Type.CANCEL);

        ScreenContext screenContext = ComponentsHelper.getScreenContext(target);
        Preconditions.checkState(screenContext != null, "Component is not attached to window");

        if (beforeActionPerformedHandler != null
                && !beforeActionPerformedHandler.beforeActionPerformed()) {
            return;
        }

        Dialogs dialogs = screenContext.getDialogs();
        Set selected = target.getSelected();
        if (CollectionUtils.isNotEmpty(selected)) {
            if (selected.size() > 1) {
                Action printSelectedAction = new AbstractAction("actions.printSelected", Status.PRIMARY) {
                    @Override
                    public void actionPerform(Component component) {
                        printSelected(selected);
                    }
                };
                printSelectedAction.setIcon("icons/reports-print-row.png");

                Action printAllAction = new AbstractAction("actions.printAll") {
                    @Override
                    public void actionPerform(Component component) {
                        printAll();
                    }
                };
                printAllAction.setIcon("icons/reports-print-all.png");

                dialogs.createOptionDialog()
                        .withCaption(messages.getMessage(ReportGuiManager.class, "notifications.confirmPrintSelectedheader"))
                        .withMessage(messages.getMessage(ReportGuiManager.class, "notifications.confirmPrintSelected"))
                        .withType(Dialogs.MessageType.CONFIRMATION)
                        .withActions(printAllAction, printSelectedAction, cancelAction)
                        .show();
            } else {
                printSelected(selected);
            }
        } else {
            if (isDataAvailable()) {
                Action yesAction = new DialogAction(DialogAction.Type.OK) {
                    @Override
                    public void actionPerform(Component component) {
                        printAll();
                    }
                };

                cancelAction.setPrimary(true);

                dialogs.createOptionDialog()
                        .withCaption(messages.getMessage(ListPrintFormAction.class, "notifications.confirmPrintAllheader"))
                        .withMessage(messages.getMessage(ListPrintFormAction.class, "notifications.confirmPrintAll"))
                        .withType(Dialogs.MessageType.CONFIRMATION)
                        .withActions(yesAction, cancelAction)
                        .show();
            } else {
                Notifications notifications = screenContext.getNotifications();
                notifications.create()
                        .withCaption(messages.getMessage(ReportGuiManager.class, "notifications.noSelectedEntity"))
                        .withType(Notifications.NotificationType.HUMANIZED)
                        .show();
            }

        }
    }

    protected boolean isDataAvailable() {
        if (target.getItems() instanceof ContainerDataUnit) {
            ContainerDataUnit unit = (ContainerDataUnit) target.getItems();
            CollectionContainer container = unit.getContainer();
            return container instanceof HasLoader && unit.getState() == BindingState.ACTIVE && !container.getItems().isEmpty();
        } else {
            CollectionDatasource ds = target.getDatasource();
            if (ds != null)
                return ds.getState() == Datasource.State.VALID && ds.size() > 0;
        }
        return false;
    }

    protected void printSelected(Set selected) {
        MetaClass metaClass;
        if (target.getItems() instanceof ContainerDataUnit) {
            ContainerDataUnit unit = (ContainerDataUnit) target.getItems();
            InstanceContainer container = unit.getContainer();
            metaClass = container.getEntityMetaClass();
        } else {
            CollectionDatasource ds = target.getDatasource();
            metaClass = ds.getMetaClass();
        }
        Window window = ComponentsHelper.getWindowNN(target);
        openRunReportScreen(window.getFrameOwner(), selected, metaClass);
    }

    protected void printAll() {
        MetaClass metaClass;
        LoadContext loadContext;

        if (target.getItems() instanceof ContainerDataUnit) {
            ContainerDataUnit unit = (ContainerDataUnit) target.getItems();
            CollectionContainer container = unit.getContainer();
            if (container instanceof CollectionPropertyContainer) {
                // as CollectionPropertyContainer does not have loader it always fetches all records,
                // so print these records as selected
                printSelected(new HashSet(container.getMutableItems()));
                return;
            }
            CollectionLoader loader = (CollectionLoader) ((HasLoader) unit.getContainer()).getLoader();
            metaClass = container.getEntityMetaClass();
            loadContext = loader.createLoadContext();
        } else {
            CollectionDatasource ds = target.getDatasource();
            metaClass = ds.getMetaClass();
            loadContext = ds.getCompiledLoadContext();
        }

        ParameterPrototype parameterPrototype = new ParameterPrototype(metaClass.getName());
        parameterPrototype.setMetaClassName(metaClass.getName());
        LoadContext.Query query = loadContext.getQuery();
        parameterPrototype.setQueryString(query.getQueryString());
        parameterPrototype.setQueryParams(query.getParameters());
        parameterPrototype.setCondition(query.getCondition());
        parameterPrototype.setSort(query.getSort());

        if (!loadContext.getView().getName().equals("")) {
            parameterPrototype.setViewName(loadContext.getView().getName());
        } else {
            parameterPrototype.setView(View.copy(loadContext.getView()));
        }

        Window window = ComponentsHelper.getWindowNN(target);
        openRunReportScreen(window.getFrameOwner(), parameterPrototype, metaClass);
    }
}
