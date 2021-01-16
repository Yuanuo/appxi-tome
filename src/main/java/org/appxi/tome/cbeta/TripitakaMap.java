package org.appxi.tome.cbeta;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TripitakaMap {
    private Map<String, Tripitaka> data;

    public TripitakaMap() {
    }

    public Map<String, Tripitaka> getDataMap() {
        if (null != this.data)
            return this.data;

        this.data = new HashMap<>(32);
        try {
            final Path dataFile = CbetaHelper.resolveData("bookdata.txt");
            Files.lines(dataFile, Charset.forName("X-UTF-16LE-BOM"))//
                    .map(Tripitaka::new) //
                    .forEach(v -> data.put(v.id, v));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this.data;
    }
}
