/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/commercial-software-license for details.
 */

package com.haulmont.reports.libintegration;

import com.haulmont.cuba.core.app.FileStorageAPI;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.*;
import com.haulmont.reports.ReportingConfig;
import com.haulmont.yarg.exception.ReportFormattingException;
import com.haulmont.yarg.formatters.factory.FormatterFactoryInput;
import com.haulmont.yarg.formatters.impl.HtmlFormatter;
import com.haulmont.yarg.structure.BandData;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.pdf.ITextFSImage;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.ITextUserAgent;
import org.xhtmlrenderer.resource.ImageResource;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class CubaHtmlFormatter extends HtmlFormatter {
    protected static final String CUBA_FONTS_DIR = "/cuba/fonts";

    public static final String FS_PROTOCOL_PREFIX = "fs://";
    public static final String WEB_APP_PREFIX = "web://";
    public static final String CORE_APP_PREFIX = "core://";

    private static final Logger log = LoggerFactory.getLogger(CubaHtmlFormatter.class);

    protected Messages messages = AppBeans.get(Messages.class);
    protected final ReportingConfig reportingConfig = AppBeans.get(Configuration.class).getConfig(ReportingConfig.class);
    protected int entityMapMaxDeep = reportingConfig.getEntityTreeModelMaxDeep();
    protected int externalImagesTimeoutSec = reportingConfig.getHtmlExternalResourcesTimeoutSec();

    public CubaHtmlFormatter(FormatterFactoryInput formatterFactoryInput) {
        super(formatterFactoryInput);
    }

    //todo degtyarjov, artamonov - get rid of custom processing of file descriptors, use field formats
    // we can append <content> with Base64 to html and put reference to <img> for html
    // and some custom reference if we need pdf and then implement ResourcesITextUserAgentCallback which will
    // take base64 from appropriate content
    @Override
    protected void renderPdfDocument(String htmlContent, OutputStream outputStream) {
        ITextRenderer renderer = new ITextRenderer();
        try {
            htmlContent = Pattern.compile("(?i)<!doctype").matcher(htmlContent).replaceAll("<!DOCTYPE");
            File tmpFile = File.createTempFile("htmlReport", ".htm");
            DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(tmpFile));
            dataOutputStream.write(htmlContent.getBytes(StandardCharsets.UTF_8));
            dataOutputStream.close();

            loadFonts(renderer);

            String url = tmpFile.toURI().toURL().toString();
            renderer.setDocument(url);

            ResourcesITextUserAgentCallback userAgentCallback =
                    new ResourcesITextUserAgentCallback(renderer.getOutputDevice());
            userAgentCallback.setSharedContext(renderer.getSharedContext());

            renderer.getSharedContext().setUserAgentCallback(userAgentCallback);

            renderer.layout();
            renderer.createPDF(outputStream);

            FileUtils.deleteQuietly(tmpFile);
        } catch (Exception e) {
            throw wrapWithReportingException("", e);
        }
    }

    protected void loadFonts(ITextRenderer renderer) {
        Configuration configuration = AppBeans.get(Configuration.class);
        GlobalConfig config = configuration.getConfig(GlobalConfig.class);
        String fontsPath = config.getConfDir() + CUBA_FONTS_DIR;

        File fontsDir = new File(fontsPath);

        loadFontsFromDirectory(renderer, fontsDir);

        ReportingConfig serverConfig = configuration.getConfig(ReportingConfig.class);
        if (StringUtils.isNotBlank(serverConfig.getPdfFontsDirectory())) {
            File systemFontsDir = new File(serverConfig.getPdfFontsDirectory());
            loadFontsFromDirectory(renderer, systemFontsDir);
        }
    }

    protected void loadFontsFromDirectory(ITextRenderer renderer, File fontsDir) {
        if (fontsDir.exists()) {
            if (fontsDir.isDirectory()) {
                File[] files = fontsDir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        String lower = name.toLowerCase();
                        return lower.endsWith(".otf") || lower.endsWith(".ttf");
                    }
                });
                for (File file : files) {
                    try {
                        // Usage of some fonts may be not permitted
                        renderer.getFontResolver().addFont(file.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
                    } catch (IOException | DocumentException e) {
                        if (StringUtils.contains(e.getMessage(), "cannot be embedded due to licensing restrictions")) {
                            log.debug(e.getMessage());
                        } else {
                            log.warn(e.getMessage());
                        }
                    }
                }
            } else
                log.warn(format("File %s is not a directory", fontsDir.getAbsolutePath()));
        } else {
            log.debug("Fonts directory does not exist: " + fontsDir.getPath());
        }
    }

    protected class ResourcesITextUserAgentCallback extends ITextUserAgent {

        public ResourcesITextUserAgentCallback(ITextOutputDevice outputDevice) {
            super(outputDevice);
        }

        @Override
        public ImageResource getImageResource(String uri) {
            if (StringUtils.startsWith(uri, FS_PROTOCOL_PREFIX)) {
                ImageResource resource;
                resource = (ImageResource) _imageCache.get(uri);
                if (resource == null) {
                    InputStream is = resolveAndOpenStream(uri);
                    if (is != null) {
                        try {
                            Image image = Image.getInstance(IOUtils.toByteArray(is));

                            scaleToOutputResolution(image);
                            resource = new ImageResource(uri, new ITextFSImage(image));
                            //noinspection unchecked
                            _imageCache.put(uri, resource);
                        } catch (Exception e) {
                            throw wrapWithReportingException(
                                    format("Can't read image file; unexpected problem for URI '%s'", uri), e);
                        } finally {
                            IOUtils.closeQuietly(is);
                        }
                    }
                }

                if (resource != null) {
                    ITextFSImage image = (ITextFSImage) resource.getImage();

                    com.lowagie.text.Image imageObject;
                    // use reflection for access to internal image
                    try {
                        Field imagePrivateField = image.getClass().getDeclaredField("_image");
                        imagePrivateField.setAccessible(true);

                        imageObject = (Image) imagePrivateField.get(image);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new ReportFormattingException("Error while clone internal image in Itext");
                    }

                    resource = new ImageResource(uri, new ITextFSImage(imageObject));
                } else {
                    resource = new ImageResource(uri, null);
                }

                return resource;
            } else if (StringUtils.startsWith(uri, WEB_APP_PREFIX) || StringUtils.startsWith(uri, CORE_APP_PREFIX)) {
                String resolvedUri = resolveServerPrefix(uri);
                return super.getImageResource(resolvedUri);
            }

            return super.getImageResource(uri);
        }

        protected void scaleToOutputResolution(Image image) {
            float factor = getSharedContext().getDotsPerPixel();
            image.scaleAbsolute(image.getPlainWidth() * factor, image.getPlainHeight() * factor);
        }

        @Override
        protected InputStream resolveAndOpenStream(String uri) {
            if (StringUtils.startsWith(uri, FS_PROTOCOL_PREFIX)) {
                String uuidString = StringUtils.substring(uri, FS_PROTOCOL_PREFIX.length());

                DataManager dataWorker = AppBeans.get(DataManager.class);
                LoadContext<FileDescriptor> loadContext = new LoadContext<>(FileDescriptor.class);
                loadContext.setView(View.LOCAL);

                UUID id = UUID.fromString(uuidString);
                loadContext.setId(id);

                FileDescriptor fd = dataWorker.load(loadContext);
                if (fd == null) {
                    throw new ReportFormattingException(
                            format("File with id [%s] has not been found in file storage", id));
                }

                FileStorageAPI storageAPI = AppBeans.get(FileStorageAPI.class);
                try {
                    return storageAPI.openStream(fd);
                } catch (FileStorageException e) {
                    throw wrapWithReportingException(
                            format("An error occurred while loading file with id [%s] from file storage", id), e);
                }
            } else if (StringUtils.startsWith(uri, WEB_APP_PREFIX) || StringUtils.startsWith(uri, CORE_APP_PREFIX)) {
                String resolvedUri = resolveServerPrefix(uri);
                return getInputStream(resolvedUri);
            } else {
                return getInputStream(uri);
            }
        }

        protected InputStream getInputStream(String uri) {
            uri = resolveURI(uri);
            InputStream inputStream;
            try {
                URL url = new URL(uri);
                URLConnection urlConnection = url.openConnection();
                urlConnection.setConnectTimeout(externalImagesTimeoutSec * 1000);
                inputStream = urlConnection.getInputStream();
            } catch (java.net.SocketTimeoutException e) {
                throw new ReportFormattingException(format("Loading resource [%s] has been stopped by timeout", uri), e);
            } catch (java.net.MalformedURLException e) {
                throw new ReportFormattingException(format("Bad URL given: [%s]", uri), e);
            } catch (FileNotFoundException e) {
                throw new ReportFormattingException(format("Resource at URL [%s] not found", uri));
            } catch (IOException e) {
                throw new ReportFormattingException(format("An IO problem occurred while loading resource [%s]", uri), e);
            }

            return inputStream;
        }
    }

    protected String resolveServerPrefix(String uri) {
        Configuration configStorage = AppBeans.get(Configuration.NAME);
        GlobalConfig globalConfig = configStorage.getConfig(GlobalConfig.class);
        String coreUrl = String.format("http://%s:%s/%s/",
                globalConfig.getWebHostName(), globalConfig.getWebPort(), globalConfig.getWebContextName());
        String webUrl = globalConfig.getWebAppUrl() + "/";
        return uri.replace(WEB_APP_PREFIX, webUrl).replace(CORE_APP_PREFIX, coreUrl);
    }

    @SuppressWarnings("unchecked")
    protected Map getTemplateModel(BandData rootBand) {
        Map model = super.getTemplateModel(rootBand);
        model.put("getMessage", (TemplateMethodModelEx) arguments -> {
            checkArgsCount("getMessage", arguments, 1, 2);
            if (arguments.size() == 1) {
                Object arg = arguments.get(0);
                if (arg instanceof WrapperTemplateModel && ((WrapperTemplateModel) arg).getWrappedObject() instanceof Enum) {
                    return messages.getMessage((Enum) ((WrapperTemplateModel) arg).getWrappedObject());
                } else {
                    throwIncorrectArgType("getMessage", 1, "Enum");
                }
            }
            if (arguments.size() == 2) {
                Object arg1 = arguments.get(0);
                Object arg2 = arguments.get(1);
                if (!(arg1 instanceof TemplateScalarModel)) {
                    throwIncorrectArgType("getMessage", 1, "String");
                }
                if (!(arg2 instanceof TemplateScalarModel)) {
                    throwIncorrectArgType("getMessage", 2, "String");
                }
                return messages.getMessage(((TemplateScalarModel) arg1).getAsString(), ((TemplateScalarModel) arg2).getAsString());
            }
            return null;
        });
        model.put("getMainMessage", (TemplateMethodModelEx) arguments -> {
            checkArgsCount("getMainMessage", arguments, 1);
            Object arg = arguments.get(0);
            if (arg instanceof TemplateScalarModel) {
                return messages.getMainMessage(((TemplateScalarModel) arg).getAsString());
            } else {
                throwIncorrectArgType("getMainMessage", 1, "String");
            }
            return null;
        });

        return model;
    }

    @Override
    protected Map getBandModel(BandData band) {
        Map<String, Object> model = new HashMap<>();

        Map<String, Object> bands = new HashMap<>();
        for (String bandName : band.getChildrenBands().keySet()) {
            List<BandData> subBands = band.getChildrenBands().get(bandName);
            List<Map> bandModels = new ArrayList<>();
            for (BandData child : subBands)
                bandModels.add(getBandModel(child));

            bands.put(bandName, bandModels);
        }
        model.put("bands", bands);
        Map<String, Object> data = new HashMap<>();
        for (String key : band.getData().keySet()) {
            if (band.getData().get(key) instanceof Enum)
                data.put(key, defaultFormat(band.getData().get(key)));
            else
                data.put(key, band.getData().get(key));
        }
        model.put("fields", data);

        return model;
    }

    protected void checkArgsCount(String methodName, List arguments, int... count) throws TemplateModelException {
        if ((arguments == null || arguments.size() == 0) && Arrays.binarySearch(count, 0) == -1)
            throw new TemplateModelException(String.format("Arguments not specified for method: %s", methodName));
        if (arguments != null && Arrays.binarySearch(count, arguments.size()) == -1) {
            throw new TemplateModelException(String.format("Incorrect arguments count: %s. Expected count: %s", arguments.size(), Arrays.toString(count)));
        }
    }

    protected void throwIncorrectArgType(String methodName, int argIdx, String type) throws TemplateModelException {
        throw new TemplateModelException(String.format("Incorrect argument[%s] type for method %s. Expected type %s", argIdx, methodName, type));
    }
}