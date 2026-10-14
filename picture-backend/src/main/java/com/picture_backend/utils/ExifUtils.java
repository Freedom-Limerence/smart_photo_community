package com.picture_backend.utils;

import cn.hutool.json.JSONUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 极简 EXIF 解析器（零第三方依赖）。
 *
 * <p>背景：COS 数据万象在压缩/生成缩略图时会剥离 EXIF，因此**必须在图片上传前、
 * 在本地临时文件阶段**解析并落库；本类只解析 JPEG 的 APP1/TIFF 结构，覆盖摄影场景需要的字段。</p>
 */
public final class ExifUtils {

    /** 解析结果 */
    public static class ExifInfo {
        private boolean available;
        private String reason;
        private String make;
        private String model;
        private String lensModel;
        private Date shootTime;
        private String orientation;
        private Double fNumber;
        private Double exposureTime;
        private Double focalLength;
        private Integer iso;
        private final Map<String, Object> raw = new LinkedHashMap<>();

        public boolean isAvailable() {
            return available;
        }

        public String getReason() {
            return reason;
        }

        public String getMake() {
            return make;
        }

        public String getModel() {
            return model;
        }

        public String getLensModel() {
            return lensModel;
        }

        public Date getShootTime() {
            return shootTime;
        }

        public String getOrientation() {
            return orientation;
        }

        public Double getFNumber() {
            return fNumber;
        }

        public Double getExposureTime() {
            return exposureTime;
        }

        public Double getFocalLength() {
            return focalLength;
        }

        public Integer getIso() {
            return iso;
        }

        public Map<String, Object> getRaw() {
            return raw;
        }

        /** 输出可入库的 JSON（含原始标签） */
        public String toJson() {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("available", available);
            json.put("parser", "local-exif-v1");
            if (!available) {
                json.put("reason", reason);
            }
            if (make != null) json.put("make", make);
            if (model != null) json.put("model", model);
            if (lensModel != null) json.put("lensModel", lensModel);
            if (shootTime != null) json.put("shootTime", cn.hutool.core.date.DateUtil.formatDateTime(shootTime));
            if (orientation != null) json.put("orientation", orientation);
            if (fNumber != null) json.put("fNumber", fNumber);
            if (exposureTime != null) json.put("exposureTime", exposureTime);
            if (focalLength != null) json.put("focalLength", focalLength);
            if (iso != null) json.put("iso", iso);
            json.put("tags", raw);
            return JSONUtil.toJsonStr(json);
        }
    }

    private ExifUtils() {
    }

    public static ExifInfo parse(File file) {
        if (file == null || !file.exists()) {
            return unavailable("文件不存在");
        }
        try (InputStream in = new FileInputStream(file)) {
            return parse(readAll(in));
        } catch (Exception e) {
            return unavailable("EXIF 解析异常: " + e.getMessage());
        }
    }

    public static ExifInfo parse(byte[] bytes) {
        ExifInfo info = new ExifInfo();
        if (bytes == null || bytes.length < 12) {
            return unavailable("图片数据为空或过小");
        }
        // JPEG: SOI = FFD8
        if ((bytes[0] & 0xFF) != 0xFF || (bytes[1] & 0xFF) != 0xD8) {
            return unavailable("非 JPEG 图片（COS 压缩后的 webp/PNG 通常不含 EXIF）");
        }
        int pos = 2;
        while (pos + 4 <= bytes.length) {
            if ((bytes[pos] & 0xFF) != 0xFF) {
                pos++;
                continue;
            }
            int marker = bytes[pos + 1] & 0xFF;
            if (marker == 0xD8 || marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) {
                pos += 2;
                continue;
            }
            if (marker == 0xDA || marker == 0xD9) {
                break; // 进入图像数据，EXIF 段已结束
            }
            int size = ((bytes[pos + 2] & 0xFF) << 8) | (bytes[pos + 3] & 0xFF);
            if (size < 2) {
                break;
            }
            int segStart = pos + 4;
            int segEnd = pos + 2 + size;
            if (segEnd > bytes.length) {
                segEnd = bytes.length;
            }
            // APP1 且以 "Exif\0\0" 开头
            if (marker == 0xE1 && segEnd - segStart > 6
                    && bytes[segStart] == 'E' && bytes[segStart + 1] == 'x'
                    && bytes[segStart + 2] == 'i' && bytes[segStart + 3] == 'f') {
                parseTiff(bytes, segStart + 6, segEnd, info);
                if (info.isAvailable()) {
                    return info;
                }
            }
            pos = segEnd;
        }
        return unavailable("图片中未找到 EXIF 信息（可能已被压缩或裁掉）");
    }

    private static void parseTiff(byte[] b, int tiffStart, int end, ExifInfo info) {
        if (tiffStart + 8 > end) {
            info.reason = "TIFF 头不完整";
            return;
        }
        boolean little = b[tiffStart] == 'I' && b[tiffStart + 1] == 'I';
        if (!little && !(b[tiffStart] == 'M' && b[tiffStart + 1] == 'M')) {
            info.reason = "非法字节序标记";
            return;
        }
        int ifd0 = tiffStart + readInt(b, tiffStart + 4, little);
        if (ifd0 + 2 > end) {
            info.reason = "IFD0 偏移越界";
            return;
        }
        int count = readShort(b, ifd0, little);
        int exifIfdOffset = -1;
        for (int i = 0; i < count; i++) {
            int entry = ifd0 + 2 + i * 12;
            if (entry + 12 > end) {
                break;
            }
            int tag = readShort(b, entry, little);
            int type = readShort(b, entry + 2, little);
            int num = readInt(b, entry + 4, little);
            int valueOffset = entry + 8;
            switch (tag) {
                case 0x010F:
                    info.make = readAscii(b, tiffStart, valueOffset, type, num, end);
                    break;
                case 0x0110:
                    info.model = readAscii(b, tiffStart, valueOffset, type, num, end);
                    break;
                case 0x0112:
                    info.orientation = orientationText(readShort(b, valueOffset, little));
                    break;
                case 0x0132:
                    info.raw.put("DateTime", readAscii(b, tiffStart, valueOffset, type, num, end));
                    break;
                // 部分工具/库会把 Exif 子 IFD 的标签直接写进 IFD0，这里一并兼容（子 IFD 的值会覆盖）
                case 0x829A:
                    info.exposureTime = readRational(b, tiffStart, valueOffset, type, little, end);
                    break;
                case 0x829D:
                    info.fNumber = readRational(b, tiffStart, valueOffset, type, little, end);
                    break;
                case 0x8827:
                    info.iso = readShort(b, valueOffset, little);
                    break;
                case 0x9003:
                    String dto0 = readAscii(b, tiffStart, valueOffset, type, num, end);
                    info.raw.put("DateTimeOriginal", dto0);
                    info.shootTime = parseExifDate(dto0);
                    break;
                case 0x920A:
                    info.focalLength = readRational(b, tiffStart, valueOffset, type, little, end);
                    break;
                case 0xA434:
                    info.lensModel = readAscii(b, tiffStart, valueOffset, type, num, end);
                    break;
                case 0x8769:
                    exifIfdOffset = tiffStart + readInt(b, valueOffset, little);
                    break;
                default:
                    break;
            }
        }
        if (exifIfdOffset > 0 && exifIfdOffset + 2 <= end) {
            int exifCount = readShort(b, exifIfdOffset, little);
            for (int i = 0; i < exifCount; i++) {
                int entry = exifIfdOffset + 2 + i * 12;
                if (entry + 12 > end) {
                    break;
                }
                int tag = readShort(b, entry, little);
                int type = readShort(b, entry + 2, little);
                int num = readInt(b, entry + 4, little);
                int valueOffset = entry + 8;
                switch (tag) {
                    case 0x829A:
                        info.exposureTime = readRational(b, tiffStart, valueOffset, type, little, end);
                        break;
                    case 0x829D:
                        info.fNumber = readRational(b, tiffStart, valueOffset, type, little, end);
                        break;
                    case 0x8827:
                        info.iso = readShort(b, valueOffset, little);
                        break;
                    case 0x9003:
                        String dto = readAscii(b, tiffStart, valueOffset, type, num, end);
                        info.raw.put("DateTimeOriginal", dto);
                        info.shootTime = parseExifDate(dto);
                        break;
                    case 0x920A:
                        info.focalLength = readRational(b, tiffStart, valueOffset, type, little, end);
                        break;
                    case 0xA434:
                        info.lensModel = readAscii(b, tiffStart, valueOffset, type, num, end);
                        break;
                    default:
                        break;
                }
            }
        }
        boolean any = info.make != null || info.model != null || info.shootTime != null
                || info.fNumber != null || info.iso != null || info.focalLength != null || info.lensModel != null;
        info.available = any;
        if (!any) {
            info.reason = "EXIF 段存在但未包含可识别的摄影字段";
        }
    }

    private static String orientationText(int value) {
        switch (value) {
            case 1:
            case 2:
            case 3:
            case 4:
                return "LANDSCAPE";
            case 5:
            case 6:
            case 7:
            case 8:
                return "PORTRAIT";
            default:
                return null;
        }
    }

    private static String readAscii(byte[] b, int tiffStart, int valueOffset, int type, int num, int end) {
        if (type != 2) {
            return null;
        }
        int start = num > 4 ? tiffStart + readInt(b, valueOffset, b[tiffStart] == 'I') : valueOffset;
        int len = Math.min(num, end - start);
        if (start < 0 || len <= 0) {
            return null;
        }
        String s = new String(b, start, len, StandardCharsets.UTF_8).trim();
        int zero = s.indexOf(0);
        if (zero >= 0) {
            s = s.substring(0, zero);
        }
        s = s.replace("\u0000", "").trim();
        return s.isEmpty() ? null : s;
    }

    private static Double readRational(byte[] b, int tiffStart, int valueOffset, int type, boolean little, int end) {
        if (type != 5 && type != 10) {
            return null;
        }
        int off = tiffStart + readInt(b, valueOffset, little);
        if (off + 8 > end) {
            return null;
        }
        long num = readInt(b, off, little) & 0xFFFFFFFFL;
        long den = readInt(b, off + 4, little) & 0xFFFFFFFFL;
        if (den == 0) {
            return null;
        }
        double v = num * 1.0 / den;
        return Math.round(v * 1000) / 1000.0;
    }

    private static Date parseExifDate(String text) {
        if (text == null || text.length() < 19) {
            return null;
        }
        try {
            // 形如 2024:05:06 07:08:09
            int year = Integer.parseInt(text.substring(0, 4));
            int month = Integer.parseInt(text.substring(5, 7));
            int day = Integer.parseInt(text.substring(8, 10));
            int hour = Integer.parseInt(text.substring(11, 13));
            int minute = Integer.parseInt(text.substring(14, 16));
            int second = Integer.parseInt(text.substring(17, 19));
            Calendar c = Calendar.getInstance();
            c.set(year, month - 1, day, hour, minute, second);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    private static int readShort(byte[] b, int offset, boolean little) {
        if (offset + 2 > b.length) {
            return 0;
        }
        int a = b[offset] & 0xFF;
        int c = b[offset + 1] & 0xFF;
        return little ? (c << 8) | a : (a << 8) | c;
    }

    private static int readInt(byte[] b, int offset, boolean little) {
        if (offset + 4 > b.length) {
            return 0;
        }
        int a = b[offset] & 0xFF;
        int c = b[offset + 1] & 0xFF;
        int d = b[offset + 2] & 0xFF;
        int e = b[offset + 3] & 0xFF;
        return little ? (e << 24) | (d << 16) | (c << 8) | a : (a << 24) | (c << 16) | (d << 8) | e;
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static ExifInfo unavailable(String reason) {
        ExifInfo info = new ExifInfo();
        info.available = false;
        info.reason = reason;
        return info;
    }
}
