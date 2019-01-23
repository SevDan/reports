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

package com.haulmont.reports.gui.datasource;

import com.haulmont.cuba.gui.data.impl.CollectionPropertyDatasourceImpl;
import com.haulmont.reports.entity.ParameterType;
import com.haulmont.reports.entity.ReportInputParameter;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public class TextInputParametersDataSource extends CollectionPropertyDatasourceImpl<ReportInputParameter, UUID> {

    @Override
    protected Collection<ReportInputParameter> getCollection() {
        return super.getCollection().stream()
                .filter(reportInputParameter -> reportInputParameter.getType().equals(ParameterType.TEXT))
                .collect(Collectors.toList());
    }
}
