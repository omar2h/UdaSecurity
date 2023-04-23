module Security {
    exports com.udacity.catpoint.security.data;
    exports com.udacity.catpoint.security.service;
    exports com.udacity.catpoint.security.application;
    requires Image;
    requires java.desktop;
    requires java.prefs;
    requires com.google.gson;
    requires com.google.common;

    opens com.udacity.catpoint.security.data to com.google.gson;
}