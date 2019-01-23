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

package com.haulmont.reports.libintegration;

import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.yarg.loaders.impl.SqlDataLoader;
import com.haulmont.yarg.structure.ReportQuery;
import com.haulmont.yarg.util.db.QueryRunner;
import com.haulmont.yarg.util.db.ResultSetHandler;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

public class CubaSqlDataLoader extends SqlDataLoader {

    public CubaSqlDataLoader(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected List runQuery(ReportQuery reportQuery, String queryString, Object[] params, ResultSetHandler<List> handler) throws SQLException {
        Persistence persistence = AppBeans.get(Persistence.class);
        QueryRunner runner = new QueryRunner(persistence.getDataSource(StoreUtils.getStoreName(reportQuery)));
        return runner.query(queryString, params, handler);
    }
}
