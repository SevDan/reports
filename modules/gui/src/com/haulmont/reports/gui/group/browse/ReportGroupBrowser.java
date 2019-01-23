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

package com.haulmont.reports.gui.group.browse;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.AbstractLookup;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.ListComponent;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.actions.CreateAction;
import com.haulmont.cuba.gui.components.actions.EditAction;
import com.haulmont.cuba.gui.components.actions.RemoveAction;
import com.haulmont.cuba.gui.data.DataSupplier;
import com.haulmont.reports.entity.Report;
import com.haulmont.reports.entity.ReportGroup;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

public class ReportGroupBrowser extends AbstractLookup {

    @Inject
    protected Table reportGroupsTable;

    @Named("reportGroupsTable.create")
    protected CreateAction createAction;

    @Named("reportGroupsTable.edit")
    protected EditAction editAction;

    @Inject
    protected Metadata metadata;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        createAction.setOpenType(WindowManager.OpenType.DIALOG);
        editAction.setOpenType(WindowManager.OpenType.DIALOG);

        reportGroupsTable.addAction(new RemoveReportGroupAction(reportGroupsTable));
    }

    protected class RemoveReportGroupAction extends RemoveAction {

        public RemoveReportGroupAction(ListComponent owner) {
            super(owner);
        }

        @Override
        public void actionPerform(Component component) {
            if (!isEnabled()) {
                return;
            }

            ReportGroup group = (ReportGroup) target.getSingleSelected();
            if (group != null) {
                if (group.getSystemFlag()) {
                    showNotification(getMessage("unableToDeleteSystemReportGroup"), NotificationType.WARNING);
                } else {
                    LoadContext<Report> loadContext = new LoadContext<>(Report.class);
                    loadContext.setView("report.view");
                    LoadContext.Query query =
                            new LoadContext.Query("select r from report$Report r where r.group.id = :groupId");
                    query.setMaxResults(1);
                    query.setParameter("groupId", group.getId());
                    loadContext.setQuery(query);

                    DataSupplier dataService = getDsContext().getDataSupplier();
                    Report report = dataService.load(loadContext);
                    if (report != null) {
                        showNotification(getMessage("unableToDeleteNotEmptyReportGroup"), NotificationType.WARNING);
                    } else {
                        super.actionPerform(component);
                    }
                }
            }
        }
    }
}