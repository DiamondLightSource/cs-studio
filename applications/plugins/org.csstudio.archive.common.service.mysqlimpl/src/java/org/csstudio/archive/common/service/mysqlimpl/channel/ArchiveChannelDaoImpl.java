/*
 * Copyright (c) 2010 Stiftung Deutsches Elektronen-Synchrotron,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
package org.csstudio.archive.common.service.mysqlimpl.channel;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.csstudio.archive.common.service.channel.ArchiveChannel;
import org.csstudio.archive.common.service.channel.ArchiveChannelId;
import org.csstudio.archive.common.service.channel.IArchiveChannel;
import org.csstudio.archive.common.service.channelgroup.ArchiveChannelGroupId;
import org.csstudio.archive.common.service.controlsystem.ArchiveControlSystem;
import org.csstudio.archive.common.service.controlsystem.ArchiveControlSystemId;
import org.csstudio.archive.common.service.controlsystem.IArchiveControlSystem;
import org.csstudio.archive.common.service.mysqlimpl.controlsystem.ArchiveControlSystemDaoImpl;
import org.csstudio.archive.common.service.mysqlimpl.dao.AbstractArchiveDao;
import org.csstudio.archive.common.service.mysqlimpl.dao.ArchiveDaoException;
import org.csstudio.archive.common.service.mysqlimpl.dao.ArchiveDaoManager;
import org.csstudio.domain.desy.system.ControlSystemType;
import org.csstudio.domain.desy.time.TimeInstant;
import org.csstudio.domain.desy.time.TimeInstant.TimeInstantBuilder;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;

/**
 * DAO implementation with simple cache (hashmap).
 *
 * @author bknerr
 * @since 09.11.2010
 */
public class ArchiveChannelDaoImpl extends AbstractArchiveDao implements IArchiveChannelDao {

    private static final String EXC_MSG = "Channel configuration retrieval from archive failed.";
    /**
     * Archive channel configuration cache.
     */
    private final Map<String, IArchiveChannel> _channelCacheByName = Maps.newHashMap();
    private final Map<ArchiveChannelId, IArchiveChannel> _channelCacheById = Maps.newHashMap();

    public static final String TAB = "channel";
    private static final String CS_TAB = ArchiveControlSystemDaoImpl.TAB;

    private final String _selectChannelPrefix =
        "SELECT " + TAB + ".id, " + TAB + ".name, " + TAB + ".datatype, " + TAB + ".group_id, " + TAB + ".last_sample_time, " +
                 CS_TAB + ".id, " + CS_TAB + ".name, " + CS_TAB + ".type " +
                "FROM " + getDaoMgr().getDatabaseName() + "." + TAB + ", " + getDaoMgr().getDatabaseName() + "." + CS_TAB;
    private final String _selectChannelSuffix = "AND " + TAB + ".control_system_id=" + CS_TAB + ".id";

    // FIXME (bknerr) : refactor into CRUD command objects with cmd factories
    // TODO (bknerr) : parameterize the database schema name via dao call
    private final String _selectChannelByNameStmt = _selectChannelPrefix + " WHERE " + TAB + ".name=? " + _selectChannelSuffix;
    private final String _selectChannelByIdStmt =   _selectChannelPrefix + " WHERE " + TAB + ".id=? " + _selectChannelSuffix;
    private final String _selectChannelsByGroupId = _selectChannelPrefix + " WHERE " + TAB + ".group_id=? " + _selectChannelSuffix +
                                                    " ORDER BY " + TAB + ".name";

    /**
     * Constructor.
     * @param the dao manager
     */
    public ArchiveChannelDaoImpl(@Nonnull final ArchiveDaoManager mgr) {
        super(mgr);
    }

    @Nonnull
    private IArchiveChannel getChannelFromResult(@Nonnull final ResultSet result) throws SQLException,
                                                                                         ClassNotFoundException,
                                                                                         ArchiveDaoException {
        // id, name, datatype, group_id, sample_mode_id, sample_period, last_sample_time
        final ArchiveChannelId id = new ArchiveChannelId(result.getLong(TAB + ".id"));
        final String name = result.getString(TAB + ".name");
        final String datatype = result.getString(TAB + ".datatype");
        final long groupId = result.getLong(TAB + ".group_id");
        //final int sampleMode = result.getInt("sample_mode_id");
        //final double samplePeriod = result.getDouble("sample_period");
        final Timestamp lastSampleTime = result.getTimestamp(TAB + ".last_sample_time");
        final TimeInstant time = lastSampleTime == null ? null :
                                                    TimeInstantBuilder.buildFromMillis(lastSampleTime.getTime());

        final ArchiveControlSystemId csId = new ArchiveControlSystemId(result.getLong(CS_TAB + ".id"));
        final String csName = result.getString(CS_TAB + ".name");
        final String csType = result.getString(CS_TAB + ".type");

        final IArchiveControlSystem cs = new ArchiveControlSystem(csId,
                                                                  csName,
                                                                  Enum.valueOf(ControlSystemType.class, csType));

        final IArchiveChannel channel =
            new ArchiveChannel(id,
                               name,
                               datatype,
                               new ArchiveChannelGroupId(groupId),
//                               new ArchiveSampleModeId(sampleMode),
//                               samplePeriod,
                               time,
                               cs);

        _channelCacheByName.put(name, channel);
        _channelCacheById.put(id, channel);
        return channel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CheckForNull
    public IArchiveChannel retrieveChannelByName(@Nonnull final String name) throws ArchiveDaoException {

        IArchiveChannel channel = _channelCacheByName.get(name);
        if (channel != null) {
            return channel;
        }

        PreparedStatement stmt = null;
        try {
            stmt = getConnection().prepareStatement(_selectChannelByNameStmt);
            stmt.setString(1, name);

            final ResultSet result = stmt.executeQuery();
            if (result.next()) {
                channel = getChannelFromResult(result);
            }
        } catch (final Exception e) {
            handleExceptions("", e);
        } finally {
            closeStatement(stmt, "Closing of statement " + _selectChannelByNameStmt + " failed.");
        }
        return channel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CheckForNull
    public IArchiveChannel retrieveChannelById(@Nonnull final ArchiveChannelId id) throws ArchiveDaoException {

        IArchiveChannel channel = _channelCacheById.get(id);
        if (channel != null) {
            return channel;
        }

        PreparedStatement stmt = null;
        try {
            stmt = getConnection().prepareStatement(_selectChannelByIdStmt);
            stmt.setInt(1, id.intValue());

            final ResultSet result = stmt.executeQuery();
            if (result.next()) {
                channel = getChannelFromResult(result);
            }
        } catch (final Exception e) {
            handleExceptions(EXC_MSG, e);
        } finally {
            closeStatement(stmt, "Closing of statement " + _selectChannelByIdStmt + " failed.");
        }
        return channel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public Collection<IArchiveChannel> retrieveChannelsByGroupId(@Nonnull final ArchiveChannelGroupId groupId) throws ArchiveDaoException {

        final Collection<IArchiveChannel> filteredList = getChannelsByGroupIdFromCache(groupId);
        if (!filteredList.isEmpty()) {
            return new ArrayList<IArchiveChannel>(filteredList);
        }
        // Nothing yet in the cache? Ask the database:
        final Map<String, IArchiveChannel> tempCache = Maps.newHashMap();
        final Map<ArchiveChannelId, IArchiveChannel> tempCacheById = Maps.newHashMap();
        PreparedStatement stmt = null;
        try {
            stmt = getConnection().prepareStatement(_selectChannelsByGroupId);
            stmt.setInt(1, groupId.intValue());

            final ResultSet result = stmt.executeQuery();
            while (result.next()) {
                final IArchiveChannel channel = getChannelFromResult(result);

                tempCache.put(channel.getName(), channel);
                tempCacheById.put(channel.getId(), channel);
            }

            _channelCacheByName.putAll(tempCache); // cache the overall result
            _channelCacheById.putAll(tempCacheById); // cache the overall result

            return tempCache.values();
        } catch (final Exception e) {
            handleExceptions(EXC_MSG, e);
        } finally {
            closeStatement(stmt, "Closing of statement " + _selectChannelsByGroupId + " failed.");
        }
        return Collections.emptyList();
    }

    @Nonnull
    private Collection<IArchiveChannel> getChannelsByGroupIdFromCache(@Nonnull final ArchiveChannelGroupId groupId) {
        final Collection<IArchiveChannel> filteredList =
            Collections2.filter(_channelCacheByName.values(), new Predicate<IArchiveChannel>() {
                @Override
                public boolean apply(@Nonnull final IArchiveChannel cacheElem) {
                    return cacheElem.getGroupId().equals(groupId);
                }
            });
        return filteredList;
    }


}
