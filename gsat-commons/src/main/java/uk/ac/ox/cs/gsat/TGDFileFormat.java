package uk.ac.ox.cs.gsat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public enum TGDFileFormat {
    DLGP(List.of("dlgp", "dlp")),
    CHASE_BENCH(List.of()),
    OWL("owl");

    private final Collection<String> fileExts;

    private TGDFileFormat(String fileExt) {
        this.fileExts = List.of(fileExt);
    }

    private TGDFileFormat(Collection<String> fileExts) {
        this.fileExts = fileExts;
    }

    public Collection<String> getFileExts() {
        return fileExts;
    }

    public static TGDFileFormat getFormatFromPath(String path) {
        for (TGDFileFormat format : TGDFileFormat.values()) {
            if (format.getFileExts().stream().anyMatch(f -> path.matches(".*\\." + f)))
                return format;
        }
        if (new File(path).isDirectory())
            return TGDFileFormat.CHASE_BENCH;
        
        return null;
    }

    public static boolean matchesAny(String path) {
        return getFormatFromPath(path) != null;
    }
    
    public static List<String> getExtensions() {
        List<String> result = new ArrayList<>();

        Arrays.stream(TGDFileFormat.values()).forEach(f -> result.addAll(f.getFileExts()));

        return result;
    }

}
