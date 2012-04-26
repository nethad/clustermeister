/*
 * Copyright 2012 The Clustermeister Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.nethad.clustermeister.provisioning.torque;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.configuration.Configuration;

/**
 *
 * @author thomas
 */
public class ConfigurationForTesting implements Configuration {

    Map<String, Object> configValues = new HashMap<String, Object>();

    public ConfigurationForTesting(Map<String, Object> configValues) {
        this.configValues = configValues;
    }
    
    @Override
    public String getString(String key, String defaultValue) {
        if (configValues.containsKey(key)) {
            return (String) configValues.get(key);
        } else {
            return defaultValue;
        }
    }

    @Override
    public int getInt(String key, int defaultValue) {
        if (configValues.containsKey(key)) {
            return (Integer) configValues.get(key);
        } else {
            return defaultValue;
        }
    }

    @Override
    public Configuration subset(String prefix) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsKey(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addProperty(String key, Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setProperty(String key, Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clearProperty(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getProperty(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Iterator<String> getKeys(String prefix) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Iterator<String> getKeys() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Properties getProperties(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getBoolean(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte getByte(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public byte getByte(String key, byte defaultValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Byte getByte(String key, Byte defaultValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getDouble(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Double getDouble(String key, Double defaultValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public float getFloat(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Float getFloat(String key, Float defaultValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getInt(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Integer getInteger(String key, Integer defaultValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLong(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLong(String key, long defaultValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Long getLong(String key, Long defaultValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public short getShort(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public short getShort(String key, short defaultValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Short getShort(String key, Short defaultValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BigDecimal getBigDecimal(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BigInteger getBigInteger(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BigInteger getBigInteger(String key, BigInteger defaultValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getString(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String[] getStringArray(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Object> getList(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Object> getList(String key, List<Object> defaultValue) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
