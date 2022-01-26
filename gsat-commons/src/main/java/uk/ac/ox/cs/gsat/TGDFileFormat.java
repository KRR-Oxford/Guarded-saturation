package uk.ac.ox.cs.gsat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum TGDFileFormat {
    DLGP("dlgp"),
    OWL("owl");

    private final String fileExt;

    private TGDFileFormat(String fileExt) {
        this.fileExt = fileExt;
    }

    public String getFileExt() {
        return fileExt;
    }

    public static TGDFileFormat getFormatFromPath(String path) {
        for (TGDFileFormat format : TGDFileFormat.values()) {
            if (path.matches(".*\\." + format.getFileExt()))
                return format;
        }
        return null;
    }

    public static List<String> getExtensions() {
        return Arrays.stream(TGDFileFormat.values()).map(f -> f.getFileExt()).collect(Collectors.toList());
    }
}
