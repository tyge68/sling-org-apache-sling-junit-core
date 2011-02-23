/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.junit.impl.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.junit.Renderer;
import org.apache.sling.junit.RequestParser;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** HTML renderer for JUnit servlet */
@Component(immediate=false)
@Service
public class JsonRenderer extends RunListener implements Renderer {

    public static final String INFO_TYPE_KEY = "INFO_TYPE";
    public static final String INFO_SUBTYPE_KEY = "INFO_SUBTYPE";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private JSONWriter writer;
    private int counter;
    
    /** @inheritDoc */
    public boolean appliesTo(RequestParser p) {
        return "json".equals(p.getExtension());
    }

    /** @inheritDoc */
    public void setup(HttpServletResponse response, String pageTitle) throws IOException, UnsupportedEncodingException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        writer = new JSONWriter(response.getWriter());
        writer.setTidy(true);
        try {
            writer.array();
        } catch(JSONException jex) {
            throw (IOException)new IOException().initCause(jex);
        }
    }

    /** @inheritDoc */
    public void cleanup() {
        if(writer != null) {
            try {
                writer.endArray();
            } catch(JSONException jex) {
                log.warn("JSONException in cleanup()", jex);
            }
        }
        writer = null;
    }

    /** @inheritDoc */
    public void info(String cssClass, String info) {
        try {
            startItem("info");
            writer.key(INFO_SUBTYPE_KEY).value(cssClass);
            writer.key("info").value(info);
            endItem();
        } catch(JSONException jex) {
            log.warn("JSONException in info()", jex);
        }
    }

    /** @inheritDoc */
    public void list(String cssClass, Collection<String> data) {
        try {
            startItem("list");
            writer.key(INFO_SUBTYPE_KEY).value(cssClass);
            writer.key("data");
            writer.array();
            for(String str : data) {
                writer.value(str);
            }
            writer.endArray();
            endItem();
        } catch(JSONException jex) {
            log.warn("JSONException in list()", jex);
        }
    }

    /** @inheritDoc */
    public void title(int level, String title) {
        // Titles are not needed in JSON
    }
    
    /** @inheritDoc */
    public void link(String info, String url, String method) {
        try {
            startItem("link");
            writer.key("info").value(info);
            writer.key("method").value(method);
            writer.key("url").value(url);
            endItem();
        } catch(JSONException jex) {
            log.warn("JSONException in link()", jex);
        }
    }

    /** @inheritDoc */
    public RunListener getRunListener() {
        return this;
    }

    @Override
    public void testStarted(Description description) throws Exception {
        super.testStarted(description);
        startItem("test");
        writer.key("description").value(description.toString());
    }
    
    @Override
    public void testFinished(Description description) throws Exception {
        super.testFinished(description);
        endItem();
    }

    
    @Override
    public void testFailure(Failure failure) throws Exception {
        writer.key("failure").value(failure.toString());
    }
    
    @Override
    public void testRunFinished(Result result) throws Exception {
        // Not needed, info is already present in the output
    }
    
    void startItem(String name) throws JSONException {
        ++counter;
        writer.object();
        writer.key(INFO_TYPE_KEY).value(name);
    }
    
    void endItem() throws JSONException {
        writer.endObject();
    }
}
