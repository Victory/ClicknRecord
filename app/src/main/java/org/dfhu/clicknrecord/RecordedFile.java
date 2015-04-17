package org.dfhu.clicknrecord;

public class RecordedFile {
    public final String absolutePath;
    public final String filename;

    public RecordedFile(String filename, String absolutePath) {
        this.filename = filename;
        this.absolutePath = absolutePath;
    }

}
