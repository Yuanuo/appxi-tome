package org.appxi.tome.cbeta;

import org.appxi.util.StringHelper;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BookMap {
    private static final Pattern VOL_REGEX = Pattern.compile("(.*)(\\(第)(\\d+)(卷-第)(\\d+)(卷.*)");
    public final TripitakaMap tripitakaMap;
    private Map<String, CbetaBook> data;
    private final Object dataInit = new Object();

    public BookMap() {
        this(new TripitakaMap());
    }

    public BookMap(TripitakaMap tripitakaMap) {
        this.tripitakaMap = tripitakaMap;
    }

    public boolean isEmpty() {
        return null == this.data || this.data.isEmpty();
    }

    public Map<String, CbetaBook> getDataMap() {
        if (null != this.data)
            return this.data;
        synchronized (dataInit) {
            if (null != this.data)
                return this.data;
            this.data = new HashMap<>(5120);
            try {
                Files.lines(CbetaHelper.resolveData("catalog.txt")).forEach(this::parseBook);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.gc();
            }
        }
        return this.data;
    }

    private void parseBook(final String line) {
        final String[] arr = line.split(", ", 8);
        final String tripitakaId = arr[0].strip();
        String catalog = arr[1].strip();
        final String group = arr[2].strip();
        if (!group.isBlank())
            catalog = StringHelper.concat(catalog, "/", group);
        final String serial = arr[3].strip();
        final String number = arr[4].strip();
        final int volsNum = Integer.parseInt(arr[5].strip());
        String name = arr[6].strip();
        final String author = arr[7].strip();

        // detect vol num from name
        final Matcher matcher = VOL_REGEX.matcher(name);
        int startVolNum = 1, endVolNum = volsNum;
        if (matcher.matches()) {
            name = matcher.group(1);
            startVolNum = Integer.parseInt(matcher.group(3));
            endVolNum = Integer.parseInt(matcher.group(5));
        }

        //
        final String bookId = StringHelper.concat(tripitakaId, number);
        CbetaBook book = data.get(bookId);
        if (null == book) {
            data.put(bookId, book = createCbetaBook());
            book.id = bookId;
            book.title = name;
            book.authorInfo = author;
            book.catalog = catalog;
            Tripitaka tripitaka = tripitakaMap.getDataMap().get(tripitakaId);
            if (null == tripitaka)
                book.location = StringHelper.concat(catalog, "/", name);
            else book.location = StringHelper.concat(tripitaka.name, "/", catalog, "/", name);
            book.path = StringHelper.concat("toc/", tripitakaId, "/", bookId, ".xml");
            book.tripitakaId = tripitakaId;
            book.number = number;
            startVolNum = 1;
        }
        book.serialVols.add(new CbetaBook.SerialVol(serial, startVolNum, endVolNum));
    }

    protected CbetaBook createCbetaBook() {
        return new CbetaBook();
    }
}
