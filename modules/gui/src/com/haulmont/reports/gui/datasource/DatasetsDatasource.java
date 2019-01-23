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

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.data.impl.CollectionPropertyDatasourceImpl;
import com.haulmont.reports.entity.BandDefinition;
import com.haulmont.reports.entity.DataSet;
import com.haulmont.reports.entity.Report;

import java.util.*;

public class DatasetsDatasource extends CollectionPropertyDatasourceImpl<DataSet, UUID> {

    @Override
    public void committed(Set<Entity> entities) {
        if (!State.VALID.equals(masterDs.getState()))
            return;
        Collection<DataSet> collection = getCollection();
        if (collection != null) {
            for (Entity entity : entities) {
                if (entity instanceof Report) {
                    for (BandDefinition definition : ((Report) entity).getBands()) {
                        if (definition.equals(masterDs.getItem())) {
                            for (DataSet dataset : definition.getDataSets()) {
                                for (DataSet item : new ArrayList<>(collection)) {
                                    if (item.equals(dataset)) {
                                        if (collection instanceof List) {
                                            List list = (List) collection;
                                            list.set(list.indexOf(item), dataset);
                                        } else if (collection instanceof Set) {
                                            Set set = (Set) collection;
                                            set.remove(item);
                                            set.add(dataset);
                                        }

                                        attachListener(dataset);
                                        if (dataset.equals(this.item)) {
                                            this.item = dataset;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        modified = false;
        clearCommitLists();
    }
}