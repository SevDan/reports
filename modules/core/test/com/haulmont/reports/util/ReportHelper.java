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
import com.haulmont.reports.app.service.ReportService;
import com.haulmont.reports.entity.BandDefinition;
import com.haulmont.reports.entity.Report;
import com.haulmont.reports.entity.ReportOutputType;
import com.haulmont.reports.entity.ReportTemplate;
import com.haulmont.reports.entity.wizard.TemplateFileType;
import com.haulmont.reports.exception.TemplateGenerationException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Collections;

/**
 * Helper bean for report tests
 */
@Component("test_ReportHelper")
public class ReportHelper {

    @Inject
    private Metadata metadata;

    public Report create(BandDefinition bandDefinition, File tpl) throws TemplateGenerationException, IOException, URISyntaxException {
        Report report = metadata.create(Report.class);
        report.setName(bandDefinition.getName());
        report.setCode(bandDefinition.getName());
        report.setIsTmp(true);
        report.setBands(Collections.singleton(bandDefinition));
        report.setDefaultTemplate(createDefaultTemplate(report, tpl));
        report.setTemplates(Collections.singletonList(report.getDefaultTemplate()));
        return report;
    }

    public ReportTemplate createDefaultTemplate(Report report, File reportTemplate) throws TemplateGenerationException, URISyntaxException, IOException {
        TemplateFileType templateType = null;
        for(TemplateFileType type: TemplateFileType.values()) {
            if (reportTemplate.getName().toLowerCase().endsWith(type.name().toLowerCase())) {
                templateType = type;
            }
        }
        ReportTemplate template = metadata.create(ReportTemplate.class);
        template.setReport(report);
        template.setCode(ReportService.DEFAULT_TEMPLATE_CODE);
        template.setName(reportTemplate.getName());
        template.setContent(Files.readAllBytes(reportTemplate.toPath()));
        template.setCustom(Boolean.FALSE);
        template.setReportOutputType(ReportOutputType.fromId(templateType.getId()));
        template.setOutputNamePattern(report.getName()
                .replace(' ', '_').toLowerCase() + '.' + templateType.name().toLowerCase());
        return template;
    }
}
