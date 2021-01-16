package org.appxi.tome.cbeta;

public class Tripitaka {
    public String id;
    public int num;
    public String nameTag, name, nameEng;

    public Tripitaka() {
    }

    public Tripitaka(String line) {
        final String[] arr = line.strip().split(",", 5);
        this.id = arr[0];
        this.num = Integer.parseInt(arr[1]);
        this.nameTag = arr[2];
        this.name = arr[3];
        this.nameEng = arr[4];
    }
}
