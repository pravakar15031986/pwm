/*
 * Password Management Servlets (PWM)
 * http://www.pwm-project.org
 *
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009-2021 The PWM Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package password.pwm.svc.pwnotify;

import password.pwm.PwmDomain;
import password.pwm.bean.SessionLabel;
import password.pwm.bean.UserIdentity;
import password.pwm.error.ErrorInformation;
import password.pwm.error.PwmError;
import password.pwm.error.PwmUnrecoverableException;
import password.pwm.ldap.LdapOperationsHelper;
import password.pwm.svc.db.DatabaseException;
import password.pwm.svc.db.DatabaseTable;
import password.pwm.util.json.JsonFactory;
import password.pwm.util.java.StringUtil;

import java.util.Optional;

class PwNotifyDbStorageService implements PwNotifyStorageService
{
    private static final String DB_STATE_STRING = "PwNotifyJobState";

    private static final DatabaseTable TABLE = DatabaseTable.PW_NOTIFY;
    private final PwmDomain pwmDomain;

    PwNotifyDbStorageService( final PwmDomain pwmDomain ) throws PwmUnrecoverableException
    {
        this.pwmDomain = pwmDomain;

        if ( !pwmDomain.getConfig().getAppConfig().hasDbConfigured() )
        {
            final String msg = "DB storage type selected, but remote DB is not configured.";
            throw PwmUnrecoverableException.newException( PwmError.ERROR_NODE_SERVICE_ERROR, msg );
        }
    }

    @Override
    public Optional<PwNotifyUserStatus> readStoredUserState(
            final UserIdentity userIdentity,
            final SessionLabel sessionLabel
    )
            throws PwmUnrecoverableException
    {
        final String guid = LdapOperationsHelper.readLdapGuidValue( pwmDomain, sessionLabel, userIdentity, true );

        if ( StringUtil.isEmpty( guid ) )
        {
            throw new PwmUnrecoverableException( PwmError.ERROR_MISSING_GUID );
        }

        final Optional<String> rawDbValue;
        try
        {
            rawDbValue = pwmDomain.getPwmApplication().getDatabaseAccessor().get( TABLE, guid );
        }
        catch ( final DatabaseException e )
        {
            throw new PwmUnrecoverableException( new ErrorInformation( PwmError.ERROR_DB_UNAVAILABLE, e.getMessage() ) );
        }

        return rawDbValue.map( s -> JsonFactory.get().deserialize( s, PwNotifyUserStatus.class ) );
    }

    @Override
    public void writeStoredUserState(
            final UserIdentity userIdentity,
            final SessionLabel sessionLabel,
            final PwNotifyUserStatus pwNotifyUserStatus
    )
            throws PwmUnrecoverableException
    {
        final String guid = LdapOperationsHelper.readLdapGuidValue( pwmDomain, sessionLabel, userIdentity, true );

        if ( StringUtil.isEmpty( guid ) )
        {
            throw new PwmUnrecoverableException( PwmError.ERROR_MISSING_GUID );
        }

        final String rawDbValue = JsonFactory.get().serialize( pwNotifyUserStatus );
        try
        {
            pwmDomain.getPwmApplication().getDatabaseAccessor().put( TABLE, guid, rawDbValue );
        }
        catch ( final DatabaseException e )
        {
            throw new PwmUnrecoverableException( new ErrorInformation( PwmError.ERROR_DB_UNAVAILABLE, e.getMessage() ) );
        }
    }

    @Override
    public PwNotifyStoredJobState readStoredJobState()
            throws PwmUnrecoverableException
    {
        try
        {
            final Optional<String> strValue = pwmDomain.getPwmApplication().getDatabaseService().getAccessor().get( DatabaseTable.PW_NOTIFY, DB_STATE_STRING );
            if ( strValue.isPresent() )
            {
                return JsonFactory.get().deserialize( strValue.get(), PwNotifyStoredJobState.class );
            }
            return new PwNotifyStoredJobState( null, null, null, null, false );
        }
        catch ( final DatabaseException e )
        {
            throw new PwmUnrecoverableException( new ErrorInformation( PwmError.ERROR_DB_UNAVAILABLE, e.getMessage() ) );
        }
    }

    @Override
    public void writeStoredJobState( final PwNotifyStoredJobState pwNotifyStoredJobState )
            throws PwmUnrecoverableException
    {
        try
        {
            final String strValue = JsonFactory.get().serialize( pwNotifyStoredJobState );
            pwmDomain.getPwmApplication().getDatabaseService().getAccessor().put( DatabaseTable.PW_NOTIFY, DB_STATE_STRING, strValue );
        }
        catch ( final DatabaseException e )
        {
            throw new PwmUnrecoverableException( new ErrorInformation( PwmError.ERROR_DB_UNAVAILABLE, e.getMessage() ) );
        }
    }

}
