package com.openexchange.ajax.framework;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.framework.AJAXRequest.Parameter;
import com.openexchange.ajax.framework.AbstractAJAXSession.AJAXSession;
import com.openexchange.ajax.writer.TaskWriter;
import com.openexchange.configuration.AJAXConfig;
import com.openexchange.sessiond.Sessiond;
import com.openexchange.tools.URLParameter;
import com.openexchange.tools.servlet.AjaxException;

public class AJAXClient extends Assert {

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog(AJAXClient.class);
    
    /**
     * To use character encoding.
     */
    private static final String ENCODING = "UTF-8";

    public static AJAXResponse execute(final AJAXSession session,
        final AJAXRequest request) throws AjaxException, IOException,
        SAXException, JSONException {
        LOG.trace("Logging in.");
        final String urlString = AJAXConfig.getProperty(AJAXConfig.Property
            .PROTOCOL) + "://" + AJAXConfig.getProperty(AJAXConfig.Property
                .HOSTNAME) + request.getServletPath();
        final WebRequest req;
        switch (request.getMethod()) {
        case GET:
            req = new GetMethodWebRequest(urlString);
            addParameter(req, session, request);
            break;
        case POST:
            req = new PostMethodWebRequest(urlString);
            addParameter(req, session, request);
            break;
        case PUT:
            req = new PutMethodWebRequest(urlString + getPUTParameter(session,
                request), createBody(request.getBody()), AJAXServlet
                .CONTENTTYPE_JAVASCRIPT);
            break;
        default:
            throw new AjaxException(AjaxException.Code.InvalidParameter, request
                .getMethod().name());
        }
        final WebResponse resp = session.getConversation().getResponse(req);
        final AJAXResponseParser parser = request.getParser();
        parser.checkResponse(resp);
        return parser.parse(resp.getText());
    }

    private static void addParameter(final WebRequest req,
        final AJAXSession session, final AJAXRequest request) {
        if (null != session.getId()) {
            req.setParameter(AJAXServlet.PARAMETER_SESSION, session.getId());
        }
        for (Parameter parameter : request.getParameters()) {
            req.setParameter(parameter.getName(), parameter.getValue());
        }
    }

    private static String getPUTParameter(final AJAXSession session,
        final AJAXRequest request) throws UnsupportedEncodingException {
        final URLParameter parameter = new URLParameter();
        if (null != session.getId()) {
            parameter.setParameter(AJAXServlet.PARAMETER_SESSION, session
                .getId());
        }
        for (Parameter param : request.getParameters()) {
            parameter.setParameter(param.getName(), param.getValue());
        }
        return parameter.getURLParameters();
    }

    private static InputStream createBody(final Object body)
        throws JSONException, UnsupportedEncodingException {
        return new ByteArrayInputStream(body.toString().getBytes(ENCODING));
    }
}
