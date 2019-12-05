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

package com.haulmont.reports.entity.wizard;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.BaseUuidEntity;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

@MetaClass(name = "report$WizardReportRegion")
@SystemLevel
public class ReportRegion extends BaseUuidEntity implements OrderableEntity {

    private static final long serialVersionUID = -3122228074679382191L;
    public static final String HEADER_BAND_PREFIX = "header";

    @MetaProperty
    @Transient
    protected ReportData reportData;
    @MetaProperty
    @Transient
    protected Boolean isTabulatedRegion;
    @MetaProperty
    @Composition
    @Transient
    protected List<RegionProperty> regionProperties = new ArrayList<>();
    @MetaProperty
    @Transient
    protected EntityTreeNode regionPropertiesRootNode;
    @MetaProperty
    @Transient
    protected Long orderNum = Long.MAX_VALUE;
    @MetaProperty
    @Transient
    protected String bandNameFromReport;

    public ReportRegion() {
    }

    public EntityTreeNode getRegionPropertiesRootNode() {
        return regionPropertiesRootNode;
    }

    public void setRegionPropertiesRootNode(EntityTreeNode regionPropertiesRootNode) {
        this.regionPropertiesRootNode = regionPropertiesRootNode;
    }

    @Override
    public Long getOrderNum() {
        return orderNum;
    }

    @Override
    public void setOrderNum(Long orderNum) {
        this.orderNum = orderNum;
    }

    public ReportData getReportData() {
        return reportData;
    }

    public void setReportData(ReportData reportData) {
        this.reportData = reportData;
    }

    public Boolean getIsTabulatedRegion() {
        return isTabulatedRegion;
    }

    public void setIsTabulatedRegion(Boolean isTabulatedRegion) {
        this.isTabulatedRegion = isTabulatedRegion;
    }

    public List<RegionProperty> getRegionProperties() {
        return regionProperties;
    }

    public void setRegionProperties(List<RegionProperty> regionProperties) {
        this.regionProperties = regionProperties;
    }

    @MetaProperty
    @Transient
    public String getName() {
        Messages messages = AppBeans.get(Messages.NAME);
        if (isTabulatedRegion()) {
            return messages.formatMessage(getClass(), "ReportRegion.tabulatedName", getOrderNum());
        } else {
            return messages.formatMessage(getClass(), "ReportRegion.simpleName", getOrderNum());
        }

    }

    @MetaProperty
    @Transient
    public String getNameForBand() {
        return StringUtils.isEmpty(bandNameFromReport) ? getRegionPropertiesRootNode().getWrappedMetaClass().getJavaClass().getSimpleName() +
                (isTabulatedRegion() ? "s" : "") +
                (getReportData().getReportRegions().size() == 1 ? "" : getOrderNum().toString()) : bandNameFromReport;
    }

    @MetaProperty
    @Transient
    public String getNameForHeaderBand() {
        return HEADER_BAND_PREFIX + getNameForBand();
    }

    public boolean isTabulatedRegion() {
        return Boolean.TRUE.equals(isTabulatedRegion);
    }

    public void setBandNameFromReport(String bandNameFromReport) {
        this.bandNameFromReport = bandNameFromReport;
    }

    public String getBandNameFromReport() {
        return bandNameFromReport;
    }
}
