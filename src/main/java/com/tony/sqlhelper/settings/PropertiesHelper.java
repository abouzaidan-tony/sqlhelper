package com.tony.sqlhelper.settings;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PropertiesHelper {

    private static Logger log = LogManager.getLogger(PropertiesHelper.class.getName());
    private static String PROPERTTIES_PATH = "datasource.properties";

    public static Properties read() {
        
        Properties prop = new Properties();
        try(InputStream inputStream = PropertiesHelper.class.getClassLoader().getResourceAsStream(PROPERTTIES_PATH)){
            prop.load(inputStream);
        } catch (IOException e) {
            log.error("Could not load properties file", e);
        }
        
        return prop;
    }
}
