package org.appxi.tome.cbeta;

import org.appxi.tome.model.Book;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CbetaBook extends Book {
    static final record SerialVol(String serial, int startVol, int endVol) implements Serializable {
    }

    private static final Object AK_AUTHORS = new Object();

    final List<SerialVol> serialVols = new ArrayList<>();

    public String tripitakaId, number;

    @Override
    public Collection<String> getPeriods() {
        ensureAuthorInfoParsed();
        return this.periods;
    }

    @Override
    public Collection<String> getAuthors() {
        ensureAuthorInfoParsed();
        return this.authors;
    }

    private void ensureAuthorInfoParsed() {
        if (!this.hasAttr(AK_AUTHORS)) {
            CbetaHelper.parseBookAuthorInfo(this);
            this.attr(AK_AUTHORS, true);
        }
    }
}
