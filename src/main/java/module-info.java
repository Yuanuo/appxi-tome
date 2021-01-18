module appxi.tome {
    requires java.logging;
    requires appxi.shared;
    requires org.jsoup;

    exports org.appxi.tome;
    exports org.appxi.tome.cbeta;
    exports org.appxi.tome.chmlib;
    exports org.appxi.tome.model;
    exports org.appxi.tome.parse;
    exports org.appxi.tome.xml;
}