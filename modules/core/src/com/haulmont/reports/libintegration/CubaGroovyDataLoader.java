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

import com.haulmont.cuba.core.global.Resources;
import com.haulmont.yarg.exception.DataLoadingException;
import com.haulmont.yarg.exception.ValidationException;
import com.haulmont.yarg.loaders.ReportDataLoader;
import com.haulmont.yarg.structure.BandData;
import com.haulmont.yarg.structure.ReportQuery;
import com.haulmont.yarg.util.groovy.Scripting;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class CubaGroovyDataLoader implements ReportDataLoader {
    protected Scripting scripting;

    @Inject
    protected Resources resources;

    @Inject
    protected GroovyScriptParametersProvider groovyScriptParametersProvider;

    @Inject
    public CubaGroovyDataLoader(Scripting scripting) {
        this.scripting = scripting;
    }

    @Override
    public List<Map<String, Object>> loadData(ReportQuery reportQuery, BandData parentBand, Map<String, Object> params) {
        try {
            String script = reportQuery.getScript();
            Map<String, Object> scriptParams = groovyScriptParametersProvider.prepareParameters(reportQuery, parentBand, params);

            script = StringUtils.trim(script);
            if (script.endsWith(".groovy")) {
                script = resources.getResourceAsString(script);
            }
            return scripting.evaluateGroovy(script, scriptParams);
        } catch (ValidationException e) {
            throw e;
        } catch (Throwable e) {
            throw new DataLoadingException(String.format("An error occurred while loading data for data set [%s]", reportQuery.getName()), e);
        }
    }
}