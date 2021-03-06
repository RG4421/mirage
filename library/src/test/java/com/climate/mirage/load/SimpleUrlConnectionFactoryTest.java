package com.climate.mirage.load;

import android.net.Uri;

import com.climate.mirage.RobolectricTest;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class SimpleUrlConnectionFactoryTest extends RobolectricTest {

    private MockWebServer mockWebServer;
    private URL baseUrl;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.play();
        baseUrl = mockWebServer.getUrl("");
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
        baseUrl = null;
        mockWebServer = null;
    }


    @Test
    public void testConnection() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("yay!"));

        SimpleUrlConnectionFactory urlFactory = new SimpleUrlConnectionFactory();
        InputStream inputStream = urlFactory.getStream(Uri.parse(baseUrl.toString()));
        Assert.assertEquals(1, mockWebServer.getRequestCount());
    }

    @Test
    public void testStatusCode() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(-1).setBody("yay!"));

        SimpleUrlConnectionFactory urlFactory = new SimpleUrlConnectionFactory();
        try {
            InputStream inputStream = urlFactory.getStream(Uri.parse(baseUrl.toString()));
            Assert.fail("Can not have a -1 status code. This should have failed");
        } catch (IOException e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void testStatusCodeUnknown() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(9650456).setBody("yay!"));

        SimpleUrlConnectionFactory urlFactory = new SimpleUrlConnectionFactory();
        try {
            InputStream inputStream = urlFactory.getStream(Uri.parse(baseUrl.toString()));
            Assert.fail("Can not have an unknown status code. This should have failed");
        } catch (IOException e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void testRedirects() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(301).
                setBody("redirect").setHeader("Location", baseUrl + "/fooey"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("yay!"));

        SimpleUrlConnectionFactory urlFactory = new SimpleUrlConnectionFactory();
        InputStream inputStream = urlFactory.getStream(Uri.parse(baseUrl.toString()));
        Assert.assertEquals(2, mockWebServer.getRequestCount());
    }

    @Test
    public void testRedirectToSame() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(301).
                setBody("redirect").setHeader("Location", baseUrl + "/fooey"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(301).
                setBody("redirect").setHeader("Location", baseUrl + "/fooey"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("yay!"));

        SimpleUrlConnectionFactory urlFactory = new SimpleUrlConnectionFactory();
        try {
            InputStream inputStream = urlFactory.getStream(Uri.parse(baseUrl.toString()));
            Assert.fail("Redirect to the same URL should fail");
        } catch (IOException e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void testRedirectToEmpty() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(301).
                setBody("redirect").setHeader("Location", ""));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("yay!"));

        SimpleUrlConnectionFactory urlFactory = new SimpleUrlConnectionFactory();
        try {
            InputStream inputStream = urlFactory.getStream(Uri.parse(baseUrl.toString()));
            Assert.fail("Redirect to a blank url");
        } catch (IOException e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void testRedirectsExceeded() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(301).
                setBody("redirect").setHeader("Location", baseUrl + "/fooey"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(301).
                setBody("redirect").setHeader("Location", baseUrl + "/fooey2"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(301).
                setBody("redirect").setHeader("Location", baseUrl + "/fooey3"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(301).
                setBody("redirect").setHeader("Location", baseUrl + "/fooey4"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(301).
                setBody("redirect").setHeader("Location", baseUrl + "/fooey5"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(301).
                setBody("redirect").setHeader("Location", baseUrl + "/fooey6"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("yay!"));

        SimpleUrlConnectionFactory urlFactory = new SimpleUrlConnectionFactory();
        try {
            InputStream inputStream = urlFactory.getStream(Uri.parse(baseUrl.toString()));
            Assert.fail("This should have thrown an IOException");
        } catch (IOException e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void testAddsAuthorization() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("yay!"));

        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer eeeekeeeeek");
        headers.put("other_header", "foo");
        headers.put("other_header2", "bar");
        SimpleUrlConnectionFactory urlFactory = new SimpleUrlConnectionFactory(headers);
        InputStream inputStream = urlFactory.getStream(Uri.parse(baseUrl.toString()));
        Assert.assertEquals(1, mockWebServer.getRequestCount());

        RecordedRequest request = mockWebServer.takeRequest();
        Assert.assertNotNull(request.getHeader("Authorization"));
        Assert.assertNotNull(request.getHeader("other_header"));
        Assert.assertNotNull(request.getHeader("other_header2"));
    }

}