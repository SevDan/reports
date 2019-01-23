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

import com.haulmont.reports.entity.ReportTemplate;
import com.haulmont.reports.entity.charts.*;
import com.haulmont.yarg.formatters.factory.FormatterFactoryInput;
import com.haulmont.yarg.formatters.impl.AbstractFormatter;
import com.haulmont.yarg.structure.BandData;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChartFormatter extends AbstractFormatter {
    public ChartFormatter(FormatterFactoryInput formatterFactoryInput) {
        super(formatterFactoryInput);
        this.rootBand = formatterFactoryInput.getRootBand();
        this.reportTemplate = formatterFactoryInput.getReportTemplate();
    }

    @Override
    public void renderDocument() {
        String chartJson = null;
        AbstractChartDescription chartDescription = ((ReportTemplate) reportTemplate).getChartDescription();
        if (chartDescription != null) {
            if (chartDescription.getType() == ChartType.PIE) {
                chartJson = convertPieChart((PieChartDescription) chartDescription);
            } else if (chartDescription.getType() == ChartType.SERIAL) {
                chartJson = convertSerialChart((SerialChartDescription) chartDescription);
            }
        }
        try {
            IOUtils.write(chartJson, outputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("An error occurred while rendering chart",e);
        }
    }

    private String convertSerialChart(SerialChartDescription description) {
        List<Map<String, Object>> data = new ArrayList<>();
        List<BandData> childrenByName = rootBand.getChildrenByName(description.getBandName());
        for (BandData bandData : childrenByName) {
            data.add(bandData.getData());
        }

        return new ChartToJsonConverter(((ReportTemplate) reportTemplate).getReport().getLocName())
                .convertSerialChart(description, data);
    }

    protected String convertPieChart(PieChartDescription description) {
        List<Map<String, Object>> data = new ArrayList<>();
        List<BandData> childrenByName = rootBand.getChildrenByName(description.getBandName());
        for (BandData bandData : childrenByName) {
            data.add(bandData.getData());
        }

        return new ChartToJsonConverter(((ReportTemplate) reportTemplate).getReport().getLocName())
                .convertPieChart(description, data);
    }
}
