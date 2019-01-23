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

package com.haulmont.reports.util;

import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.reports.entity.BandDefinition;
import com.haulmont.reports.entity.DataSet;
import com.haulmont.reports.entity.DataSetType;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Class presents factory bean for create {@link DataSet} instance with basic filled attributes
 */
@Component("report_DataSetFactory")
public class DataSetFactory {

    @Inject
    protected Metadata metadata;

    /**
     * Methods create {@link DataSet} instance with basic filled
     * @param dataBand with some filled attributes
     * @return new instance of {@link DataSet}
     */
    public DataSet createEmptyDataSet(BandDefinition dataBand) {
        checkNotNull(dataBand);

        DataSet dataSet = metadata.create(DataSet.class);
        dataSet.setBandDefinition(dataBand);
        dataSet.setType(DataSetType.GROOVY);
        dataSet.setText("return [[:]]");
        return dataSet;
    }
}
