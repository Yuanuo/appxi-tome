package org.appxi.tome.cbeta;

import org.appxi.tome.model.Book;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CbetaBook extends Book {
    static final record SerialVol(String serial, int startVol, int endVol) implements Serializable {
    }

    private static final Object AK_AUTHORS = new Object();

    final List<SerialVol> serialVols = new ArrayList<>();

    public String tripitakaId, number;

    public String authorInfo() {
        if (!this.hasAttr(AK_AUTHORS)) {
            CbetaHelper.parseBookAuthorInfo(this);
            this.attr(AK_AUTHORS, true);
        }
        return this.authorInfo;
    }
}
