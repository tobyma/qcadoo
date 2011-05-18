package com.qcadoo.view.internal.components.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.qcadoo.localization.api.utils.DateUtils;
import com.qcadoo.tenant.api.MultiTenantUtil;

@Service
public class FileService {

    private static final Logger LOG = LoggerFactory.getLogger(FileService.class);

    private final String fileUrlPrefix = "/files/";

    private File uploadDirectory;

    @Value("${reportPath}")
    public void setUploadDirectory(final String uploadDirectory) {
        this.uploadDirectory = new File(uploadDirectory);
    }

    public String getName(final String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        return path.substring(path.lastIndexOf('/') + 15);
    }

    public String getLastModificationDate(final String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        Date date = new Date(Long.valueOf(path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('/') + 14)));
        return new SimpleDateFormat(DateUtils.DATE_FORMAT).format(date);
    }

    public String getUrl(final String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        return fileUrlPrefix + path.substring(uploadDirectory.getAbsolutePath().length() + 1);
    }

    public String getPathFromUrl(final String url) {
        return uploadDirectory.getAbsolutePath() + "/" + url.substring(url.indexOf('/') + fileUrlPrefix.length() - 1);
    }

    public InputStream getInputStream(final String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        try {
            return new FileInputStream(new File(path));
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public String upload(final MultipartFile multipartFile) throws IOException {
        File file = getFileFromFilename(multipartFile.getOriginalFilename());

        OutputStream output = null;

        try {
            output = new FileOutputStream(file);
            IOUtils.copy(multipartFile.getInputStream(), output);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            IOUtils.closeQuietly(output);
            throw e;
        }

        return file.getAbsolutePath();
    }

    private File getFileFromFilename(final String filename) {
        String date = Long.toString(System.currentTimeMillis());
        File directory = new File(uploadDirectory, MultiTenantUtil.getCurrentTenantId() + "/" + date.charAt(date.length() - 1)
                + "/" + date.charAt(date.length() - 2) + "/");
        directory.mkdirs();
        return new File(directory, date + "_" + getNormalizedFileName(filename));
    }

    private String getNormalizedFileName(final String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.]+", "_");
    }

    public String getContentType(final String path) {
        return new MimetypesFileTypeMap().getContentType(new File(path));
    }

    public int getTenantId(final String path) {
        String part = path.substring(uploadDirectory.getAbsolutePath().length() + 1);
        return Integer.valueOf(part.substring(0, part.indexOf("/")));
    }

}