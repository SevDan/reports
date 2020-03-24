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

import com.haulmont.chile.core.model.Instance;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.sys.serialization.SerializationSupport;
import com.haulmont.reports.app.EntityMap;
import com.haulmont.reports.entity.CubaTableData;
import com.haulmont.reports.entity.ReportTemplate;
import com.haulmont.reports.entity.table.TemplateTableBand;
import com.haulmont.reports.entity.table.TemplateTableColumn;
import com.haulmont.reports.entity.table.TemplateTableDescription;
import com.haulmont.reports.exception.ReportingException;
import com.haulmont.yarg.exception.ReportFormattingException;
import com.haulmont.yarg.formatters.factory.FormatterFactoryInput;
import com.haulmont.yarg.formatters.impl.AbstractFormatter;
import com.haulmont.yarg.structure.BandData;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.*;

import static com.haulmont.reports.app.EntityMap.INSTANCE_NAME_KEY;
import static com.haulmont.reports.entity.wizard.ReportRegion.HEADER_BAND_PREFIX;

public class CubaTableFormatter extends AbstractFormatter {

    protected MessageTools messageTools;
    protected Metadata metadata;
    protected MetadataTools metadataTools;

    public CubaTableFormatter(FormatterFactoryInput formatterFactoryInput) {
        super(formatterFactoryInput);
        messageTools = AppBeans.get(MessageTools.class);
        metadata = AppBeans.get(Metadata.class);
    }

    @Override
    public void renderDocument() {
        CubaTableData dto = transformData(rootBand);
        byte[] serializedData = SerializationSupport.serialize(dto);
        try {
            IOUtils.write(serializedData, outputStream);
        } catch (IOException e) {
            throw new RuntimeException("An error occurred while rendering chart", e);
        }
    }

    protected CubaTableData transformData(BandData rootBand) {
        TemplateTableDescription templateTableDescription = ((ReportTemplate) reportTemplate).getTemplateTableDescription();

        Map<String, List<KeyValueEntity>> transformedData = new LinkedHashMap<>();
        Map<String, Set<CubaTableData.ColumnInfo>> headerMap = new HashMap<>();
        Map<String, List<BandData>> childrenBands = rootBand.getChildrenBands();

        if (templateTableDescription.getTemplateTableBands().size() > 0) {
            return getCubaTableData(templateTableDescription, transformedData, headerMap, childrenBands);
        } else {
            return getCubaTableData(transformedData, headerMap, childrenBands);
        }
    }

    private CubaTableData getCubaTableData(Map<String, List<KeyValueEntity>> transformedData,
                                           Map<String, Set<CubaTableData.ColumnInfo>> headerMap,
                                           Map<String, List<BandData>> childrenBands) {
        childrenBands.forEach((bandName, bandDataList) -> {
            if (bandName.startsWith(HEADER_BAND_PREFIX)) {
                return;
            }
            List<KeyValueEntity> entities = new ArrayList<>();
            Set<CubaTableData.ColumnInfo> headers = new HashSet<>();
            Set<String> emptyHeaders = new LinkedHashSet<>();

            bandDataList.forEach(bandData -> {
                Map<String, Object> data = bandData.getData();
                final Instance instance;
                final String pkName;
                final boolean pkInView;

                if (data instanceof EntityMap) {
                    instance = ((EntityMap) data).getInstance();
                    pkName = metadata.getTools().getPrimaryKeyName(instance.getMetaClass());
                    View view = ((EntityMap) data).getView();
                    pkInView = view != null && pkName != null && view.containsProperty(pkName);
                } else {
                    instance = null;
                    pkName = null;
                    pkInView = false;
                }

                KeyValueEntity entityRow = new KeyValueEntity();

                data.forEach((name, value) -> {
                    if (INSTANCE_NAME_KEY.equals(name)) {
                        return;
                    }
                    if (checkAddHeader(pkName, pkInView, name)) {
                        if (instance != null) {
                            name = messageTools.getPropertyCaption(instance.getMetaClass(), name);
                        }

                        checkInstanceNameLoaded(value);
                        String transformationKey = transformationKey(name);
                        if (isFormat(bandName, name)) {
                            String formattedValue = getFormattedValue(bandData.getName(), name, value);
                            entityRow.setValue(transformationKey, formattedValue);
                        } else {
                            entityRow.setValue(transformationKey, value);
                        }
                    }
                });

                if (headers.isEmpty() || headers.size() < data.size()) {
                    data.forEach((name, value) -> {
                        if (INSTANCE_NAME_KEY.equals(name)) {
                            return;
                        }
                        if (checkAddHeader(pkName, pkInView, name)) {
                            if (instance != null) {
                                name = messageTools.getPropertyCaption(instance.getMetaClass(), name);
                            }

                            if (name != null && value != null) {
                                Class valueClass = getColumnClass(bandData.getName(), name, value);
                                headers.add(new CubaTableData.ColumnInfo(transformationKey(name), valueClass, name));
                            }
                            if (name != null && value == null) {
                                emptyHeaders.add(transformationKey(name));
                            }
                        }

                    });
                }
                entities.add(entityRow);
            });

            emptyHeaders.forEach(header -> {
                if (!containsHeader(headers, header))
                    headers.add(new CubaTableData.ColumnInfo(header, String.class, header));
            });

            headers.removeIf(header -> containsLowerCaseDuplicate(header, headers));

            transformedData.put(bandName, entities);
            headerMap.put(bandName, headers);
        });

        return new CubaTableData(transformedData, headerMap);
    }

    protected CubaTableData getCubaTableData(TemplateTableDescription templateTableDescription,
                                             Map<String, List<KeyValueEntity>> transformedData,
                                             Map<String, Set<CubaTableData.ColumnInfo>> headerMap,
                                             Map<String, List<BandData>> childrenBands) {
        for (TemplateTableBand band : templateTableDescription.getTemplateTableBands()) {
            String bandName = band.getBandName();

            if (bandName.startsWith(HEADER_BAND_PREFIX)) {
                break;
            }

            List<BandData> bandDataList = childrenBands.get(bandName);
            if (bandDataList == null) {
                throw new ReportingException(String.format("Report template has an unknown band [%s]", bandName));
            }

            List<KeyValueEntity> entities = new ArrayList<>();
            Set<CubaTableData.ColumnInfo> headers = new LinkedHashSet<>();

            bandDataList.forEach(bandData -> {
                Map<String, Object> data = bandData.getData();
                final String pkName;
                final boolean pkInView;

                if (data instanceof EntityMap) {
                    Instance instance = ((EntityMap) data).getInstance();
                    pkName = metadata.getTools().getPrimaryKeyName(instance.getMetaClass());
                    View view = ((EntityMap) data).getView();
                    pkInView = view != null && pkName != null && view.containsProperty(pkName);
                } else {
                    pkName = null;
                    pkInView = false;
                }

                KeyValueEntity entityRow = new KeyValueEntity();

                for (TemplateTableColumn column : band.getColumns()) {
                    String key = column.getKey();
                    Object value = data.get(key);

                    if (INSTANCE_NAME_KEY.equals(key)) {
                        return;
                    }

                    if (checkAddHeader(pkName, pkInView, key)) {
                        checkInstanceNameLoaded(value);

                        String transformationKey = transformationKey(key);
                        if (isFormat(bandName, key)) {
                            String formattedValue = getFormattedValue(bandName, key, value);
                            entityRow.setValue(transformationKey, formattedValue);
                        } else {
                            entityRow.setValue(transformationKey, value);
                        }
                    }
                }

                if (headers.isEmpty() || headers.size() < data.size()) {
                    for (TemplateTableColumn column : band.getColumns()) {
                        String key = column.getKey();
                        Object value = data.get(key);

                        if (INSTANCE_NAME_KEY.equals(key)) {
                            return;
                        }
                        if (checkAddHeader(pkName, pkInView, key)) {

                            String transformationKey = transformationKey(key);
                            if (value != null) {
                                Class valueClass = getColumnClass(bandName, key, value);
                                headers.add(new CubaTableData.ColumnInfo(transformationKey, valueClass, column.getCaption(), column.getPosition()));
                            } else {
                                headers.add(new CubaTableData.ColumnInfo(transformationKey, String.class, column.getCaption(), column.getPosition()));
                            }
                        }
                    }
                }
                entities.add(entityRow);
            });

            headers.removeIf(header -> containsLowerCaseDuplicate(header, headers));

            transformedData.put(bandName, entities);
            headerMap.put(bandName, headers);
        }
        return new CubaTableData(transformedData, headerMap);
    }

    private String getFormattedValue(String bandName, String name, Object value) {
        return formatValue(value, name, generateFullParameterName(bandName, name));
    }

    private Class getColumnClass(String bandName, String parameterName, Object value) {
        return isFormat(bandName, parameterName) ? String.class : value.getClass();
    }

    private boolean isFormat(String bandName, String parameterName) {
        String format = getFormatString(parameterName, generateFullParameterName(bandName, parameterName));
        return format != null;
    }

    private String generateFullParameterName(String bandName, String parameterName) {
        return bandName + "." + parameterName;
    }

    private boolean checkAddHeader(String pkName, boolean pkInView, String name) {
        return pkName == null || !pkName.equals(name) || pkInView;
    }

    protected boolean containsLowerCaseDuplicate(CubaTableData.ColumnInfo columnInfo, Set<CubaTableData.ColumnInfo> headers) {
        if (!columnInfo.getKey().equals(columnInfo.getKey().toUpperCase()))
            return false;

        for (CubaTableData.ColumnInfo header : headers) {
            if (!columnInfo.equals(header)
                    && header.getKey().toUpperCase().equals(columnInfo.getKey())
                    && header.getCaption().toUpperCase().equals(columnInfo.getCaption()))
                return true;
        }
        return false;
    }

    protected void checkInstanceNameLoaded(Object value) {
        if (!(value instanceof Entity || value instanceof EntityMap))
            return;

        if (value instanceof EntityMap)
            value = ((EntityMap) value).getInstance();

        try {
            metadataTools.getInstanceName((Entity) value);
        } catch (RuntimeException e) {
            throw new ReportFormattingException("Cannot fetch instance name for entity " + value.getClass()
                    + ". Please add all attributes used at instance name to report configuration.", e);
        }
    }

    protected boolean containsHeader(Set<CubaTableData.ColumnInfo> headers, String header) {
        for (CubaTableData.ColumnInfo columnInfo : headers) {
            if (columnInfo.getKey().equals(header))
                return true;
        }
        return false;
    }

    private String transformationKey(String key) {
        return key.replace('.', '-');
    }
}
