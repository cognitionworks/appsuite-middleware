/*
 *
 *    OPEN-XCHANGE legal information
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    In some countries OX, OX Open-Xchange, open xchange and OXtender
 *    as well as the corresponding Logos OX Open-Xchange and OX are registered
 *    trademarks of the OX Software GmbH group of companies.
 *    The use of the Logos is not covered by the GNU General Public License.
 *    Instead, you are allowed to use these Logos according to the terms and
 *    conditions of the Creative Commons License, Version 2.5, Attribution,
 *    Non-commercial, ShareAlike, and the interpretation of the term
 *    Non-commercial applicable to the aforementioned license is published
 *    on the web site http://www.open-xchange.com/EN/legal/index.html.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original and derivative code shall remain
 *    under the copyright of the copyright holder(s) and/or original author(s)per
 *    the Attribution and Assignment Agreement that can be located at
 *    http://www.open-xchange.com/EN/developer/. The contributing author shall be
 *    given Attribution for the derivative code and a license granting use.
 *
 *     Copyright (C) 2016-2020 OX Software GmbH
 *     Mail: info@open-xchange.com
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License, Version 2 as published
 *     by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *     or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *     for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc., 59
 *     Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package com.openexchange.userfeedback.starrating.v1;

import static com.openexchange.java.Autoboxing.L;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.common.hash.Hashing;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.java.AsciiReader;
import com.openexchange.java.AsciiWriter;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.userfeedback.AbstractJSONFeedbackType;
import com.openexchange.userfeedback.Feedback;
import com.openexchange.userfeedback.FeedbackMetaData;
import com.openexchange.userfeedback.exception.FeedbackExceptionCodes;
import com.openexchange.userfeedback.export.ExportResultConverter;
import com.openexchange.userfeedback.fields.UserFeedbackField;
import com.openexchange.userfeedback.starrating.exception.StarRatingExceptionCodes;

/**
 * {@link StarRatingV1}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.4
 */
public class StarRatingV1 extends AbstractJSONFeedbackType {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(StarRatingV1.class);

    private static final String TYPE = "star-rating-v1";
    private static final String INSERT_SQL = "INSERT INTO feedback_star_rating_v1 (data) VALUES (?)";
    private static final String SELECT_SQL = "SELECT id, data FROM feedback_star_rating_v1 WHERE id IN (";
    private static final String DELETE_SQL = "DELETE FROM feedback_star_rating_v1 WHERE id = ?";

    @Override
    protected void validate(JSONObject jsonFeedback) throws OXException {
        if (!jsonFeedback.has(StarRatingV1ExportFields.SCORE.getName())) {
            throw StarRatingExceptionCodes.PARAMETER_MISSING.create(StarRatingV1ExportFields.SCORE.getName());
        }

        try {
            String score = jsonFeedback.getString(StarRatingV1ExportFields.SCORE.getName());
            if (Strings.isEmpty(score)) {
                throw StarRatingExceptionCodes.INVALID_SCORE_TYPE.create(score);
            }
            long scoreInt = Long.valueOf(score).longValue();
            if (scoreInt < 1) {
                throw StarRatingExceptionCodes.INVALID_SCORE_VALUE.create(L(scoreInt));
            }
        } catch (JSONException e) {
            LOG.error("Unable to retrieve 'score' from feedback.", e);
            throw StarRatingExceptionCodes.PARAMETER_MISSING.create(StarRatingV1ExportFields.SCORE.getName());
        } catch (NumberFormatException e) {
            LOG.error("Unable to parse 'score' value from feedback.", e);
            throw StarRatingExceptionCodes.BAD_PARAMETER.create(StarRatingV1ExportFields.SCORE.getName());
        }
    }

    @Override
    public long storeFeedbackInternal(JSONObject jsonFeedback, Connection con) throws OXException {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
            setBinaryStream(jsonFeedback, stmt, 1);
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }

            return -1;
        } catch (SQLException e) {
            throw FeedbackExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    private void setBinaryStream(JSONObject jObject, PreparedStatement stmt, int... positions) throws OXException {
        try {
            JSONObject json = null != jObject ? jObject : new JSONObject(0);
            ByteArrayOutputStream buf = Streams.newByteArrayOutputStream(65536);
            json.write(new AsciiWriter(buf), true);

            if (positions.length == 1) {
                stmt.setBinaryStream(positions[0], Streams.asInputStream(buf));
            } else {
                byte[] data = buf.toByteArray();
                buf = null; // might help GC
                for (int pos : positions) {
                    stmt.setBytes(pos, data);
                }
            }
        } catch (SQLException | JSONException e) {
            throw FeedbackExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public ExportResultConverter getFeedbacks(List<FeedbackMetaData> feedbackMetaData, Connection con) throws OXException {
        return getFeedbacks(feedbackMetaData, con, Collections.<String, String> emptyMap());
    }

    @Override
    public ExportResultConverter getFeedbacks(List<FeedbackMetaData> feedbackMetaData, Connection con, Map<String, String> configuration) throws OXException {
        if (feedbackMetaData.size() == 0) {
            return createExportObject(Collections.<Feedback> emptyList(), Collections.<String, String> emptyMap());
        }

        ResultSet rs = null;
        PreparedStatement stmt = null;
        Map<Long, Feedback> feedbacks = new HashMap<>();
        try {
            String sql = Databases.getIN(SELECT_SQL, feedbackMetaData.size());
            stmt = con.prepareStatement(sql);
            int x = 1;
            for (FeedbackMetaData meta : feedbackMetaData) {
                stmt.setLong(x++, meta.getTypeId());
                feedbacks.put(L(meta.getTypeId()), Feedback.builder(meta).build());
            }
            rs = stmt.executeQuery();

            while (rs.next()) {
                long id = rs.getLong(1);
                try {
                    Feedback current = feedbacks.get(L(id));
                    enrichContent(new AsciiReader(rs.getBinaryStream(2)), current);
                } catch (JSONException e) {
                    LOG.error("Unable to read feedback with id {}. Won't return it.", L(id), e);
                }
            }
            SortedSet<Feedback> sorted = sort(feedbacks.values());
            return createExportObject(sorted, configuration);
        } catch (SQLException e) {
            throw FeedbackExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    private SortedSet<Feedback> sort(Collection<Feedback> collection) {
        Comparator<Feedback> comparator = new Comparator<Feedback>() {

            @Override
            public int compare(Feedback o1, Feedback o2) {
                if (o1.getDate() < o2.getDate()) {
                    return -1;
                }
                return 1;

            }
        };
        SortedSet<Feedback> sorted = new TreeSet<Feedback>(comparator);
        sorted.addAll(collection);
        return sorted;
    }

    /**
     * Adds required export fields to content.
     *
     * @param asciiReader The initial content
     * @param current The feedback object to update
     * @throws JSONException
     */
    private void enrichContent(AsciiReader asciiReader, Feedback current) throws JSONException {
        JSONObject content = new JSONObject(asciiReader);

        Instant ofEpochMilli = Instant.ofEpochMilli(current.getDate());
        LocalDateTime date = LocalDateTime.ofInstant(ofEpochMilli, ZoneId.of("Z"));
        content.put("date", date.format(dateTimeFormatter));

        String userContextTupel = current.getCtxId() + ":" + current.getUserId();
        @SuppressWarnings("deprecation") String hashedTupel = Hashing.md5().hashString(userContextTupel, StandardCharsets.UTF_8).toString();
        content.put("user", hashedTupel);
        content.put("server_version", current.getServerVersion());
        content.put("client_version", current.getUiVersion());
        current.setContent(content);
    }

    private ExportResultConverter createExportObject(Collection<Feedback> feedbacks, Map<String, String> configuration) {
        ExportResultConverter converter = new StarRatingV1ExportResultConverter(feedbacks, configuration);
        return converter;
    }

    @Override
    public void deleteFeedbacks(List<Long> ids, Connection con) throws OXException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(DELETE_SQL);
            for (Long l : ids) {
                stmt.setLong(1, l.longValue());
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            throw FeedbackExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    protected List<UserFeedbackField> getRequiredFields() throws OXException {
        return StarRatingV1ExportFields.getFieldsRequiredByClient();
    }

}
