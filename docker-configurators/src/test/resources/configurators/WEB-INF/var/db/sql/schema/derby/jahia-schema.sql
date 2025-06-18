
    alter table jahia_acl_entries 
        drop constraint FKDBE729858C498B01;

    alter table jahia_acl_names 
        drop constraint FK3C5F357D48212BE1;

    alter table jahia_app_def 
        drop constraint FKE6C84991455C901A;

    alter table jahia_appdef_prop 
        drop constraint FK8D4A0C287FCEE0FB;

    alter table jahia_apps_share 
        drop constraint FK7FE512E81F78D07;

    alter table jahia_apps_share 
        drop constraint FK7FE512E83D3F9BB0;

    alter table jahia_audit_log 
        drop constraint FKF4669B0AC75E28FD;

    alter table jahia_category 
        drop constraint FKAFAF3D94348193C9;

    alter table jahia_ctn_def_properties 
        drop constraint FKA2522525B1CEB792;

    alter table jahia_ctn_entries 
        drop constraint FK6719F01827313F9D;

    alter table jahia_ctn_lists 
        drop constraint FKB490111D2A3FB9A2;

    alter table jahia_ctn_struct 
        drop constraint FKE9FE8F4D62299E65;

    alter table jahia_ctndef_prop 
        drop constraint FK5FB05824447AD44A;

    alter table jahia_fields_data 
        drop constraint FK891B251A291A9BF4;

    alter table jahia_fields_def_extprop 
        drop constraint FKC2B7575A477EEAEC;

    alter table jahia_grps 
        drop constraint FKE530C7C492027B41;

    alter table jahia_nstep_workflowinstance 
        drop constraint FKDA6D7CCF801AE453;

    alter table jahia_nstep_workflowinstance 
        drop constraint FKDA6D7CCFA3F4D577;

    alter table jahia_nstep_workflowinstance 
        drop constraint FKDA6D7CCFED90E370;

    alter table jahia_nstep_workflowstep 
        drop constraint FK6A6E1C067F20B53;

    alter table jahia_obj 
        drop constraint FKF6E0A6A143AACCE0;

    alter table jahia_pages_data 
        drop constraint FKB5B3A65BFC25DDC3;

    alter table jahia_pages_def 
        drop constraint FK1EA2B334B5FF0C79;

    alter table jahia_pages_def_prop 
        drop constraint FK8840898E47E25CC;

    alter table jahia_pwd_policy_rule_params 
        drop constraint FKBE451EF45A0DB19B;

    alter table jahia_pwd_policy_rules 
        drop constraint FK2BC650026DA1D1E6;

    alter table jahia_retrule 
        drop constraint FK578E2BC72D76FCE6;

    alter table jahia_retrule_range 
        drop constraint FK688A96C57D611258;

    alter table jahia_savedsearch 
        drop constraint FK782342991382CE9E;

    alter table jahia_savedsearch 
        drop constraint FK78234299ACC6817;

    alter table jahia_site_lang_list 
        drop constraint FK1DDBC16D7EED26D3;

    alter table jahia_site_lang_maps 
        drop constraint FK1DDC17667EED26D3;

    alter table jahia_sites_grps 
        drop constraint FK7B245597F46755FE;

    alter table jahia_sites_grps 
        drop constraint FK7B24559790F996AC;

    alter table jahia_sites_users 
        drop constraint FKEA2BF1BF6CF683C0;

    drop table jahia_acl;

    drop table jahia_acl_entries;

    drop table jahia_acl_names;

    drop table jahia_app_def;

    drop table jahia_appdef_prop;

    drop table jahia_appentry;

    drop table jahia_apps_share;

    drop table jahia_audit_log;

    drop table jahia_bigtext_data;

    drop table jahia_category;

    drop table jahia_category_prop;

    drop table jahia_ctn_def;

    drop table jahia_ctn_def_properties;

    drop table jahia_ctn_entries;

    drop table jahia_ctn_lists;

    drop table jahia_ctn_struct;

    drop table jahia_ctndef_prop;

    drop table jahia_ctnentries_prop;

    drop table jahia_ctnlists_prop;

    drop table jahia_db_test;

    drop table jahia_fieldreference;

    drop table jahia_fields_data;

    drop table jahia_fields_def;

    drop table jahia_fields_def_extprop;

    drop table jahia_fields_prop;

    drop table jahia_grp_access;

    drop table jahia_grp_prop;

    drop table jahia_grps;

    drop table jahia_indexingjobs;

    drop table jahia_indexingjobsserver;

    drop table jahia_installedpatch;

    drop table jahia_languages_states;

    drop table jahia_link;

    drop table jahia_link_metadata;

    drop table jahia_locks_non_excl;

    drop table jahia_nstep_workflow;

    drop table jahia_nstep_workflowhistory;

    drop table jahia_nstep_workflowinstance;

    drop table jahia_nstep_workflowstep;

    drop table jahia_nstep_workflowuser;

    drop table jahia_obj;

    drop table jahia_pages_data;

    drop table jahia_pages_def;

    drop table jahia_pages_def_prop;

    drop table jahia_pages_prop;

    drop table jahia_pages_users_prop;

    drop table jahia_pwd_policies;

    drop table jahia_pwd_policy_rule_params;

    drop table jahia_pwd_policy_rules;

    drop table jahia_reference;

    drop table jahia_resources;

    drop table jahia_retrule;

    drop table jahia_retrule_range;

    drop table jahia_retruledef;

    drop table jahia_savedsearch;

    drop table jahia_savedsearchview;

    drop table jahia_serverprops;

    drop table jahia_site_lang_list;

    drop table jahia_site_lang_maps;

    drop table jahia_site_prop;

    drop table jahia_sites;

    drop table jahia_sites_grps;

    drop table jahia_sites_users;

    drop table jahia_subscriptions;
    
    drop table jahia_url_mapping;

    drop table jahia_user_prop;

    drop table jahia_users;

    drop table jahia_version;

    drop table jahia_workflow;

    create table jahia_acl (
        id_jahia_acl integer not null,
        inheritance_jahia_acl integer,
        hasentries_jahia_acl integer,
        parent_id_jahia_acl integer,
        picked_id_jahia_acl integer,
        primary key (id_jahia_acl)
    );

    create table jahia_acl_entries (
        id_jahia_acl integer not null,
        type_jahia_acl_entries integer not null,
        target_jahia_acl_entries varchar(50) not null,
        entry_state_jahia_acl_entries integer not null,
        entry_trist_jahia_acl_entries integer not null,
        primary key (id_jahia_acl, type_jahia_acl_entries, target_jahia_acl_entries)
    );

    create table jahia_acl_names (
        acl_name varchar(255) not null,
        acl_id integer,
        primary key (acl_name)
    );

    create table jahia_app_def (
        id_jahia_app_def integer not null,
        name_jahia_app_def varchar(250),
        context_jahia_app_def varchar(250),
        visible_jahia_app_def integer,
        shared_jahia_app_def integer,
        rights_jahia_app_def integer,
        filename_jahia_app_def varchar(250),
        desc_jahia_app_def varchar(250),
        type_jahia_app_def varchar(30),
        primary key (id_jahia_app_def)
    );

    create table jahia_appdef_prop (
        appdefid_appdef_prop integer not null,
        propname_appdef_prop varchar(250) not null,
        propvalue_appdef_prop varchar(250),
        primary key (appdefid_appdef_prop, propname_appdef_prop)
    );

    create table jahia_appentry (
        id_jahia_appentry integer not null,
        appid_jahia_appentry integer,
        defname_jahia_appentry varchar(250),
        rights_jahia_appentry integer,
        reskeyname_jahia_appentry varchar(250),
        expirationTime_jahia_appentry integer,
        cacheScope_jahia_appentry varchar(250),
        primary key (id_jahia_appentry)
    );

    create table jahia_apps_share (
        definition integer not null,
        site integer not null,
        primary key (definition, site)
    );

    create table jahia_audit_log (
        id_jahia_audit_log integer not null,
        time_jahia_audit_log bigint,
        username_jahia_audit_log varchar(50),
        objecttype_jahia_audit_log integer,
        objectid_jahia_audit_log integer,
        parenttype_jahia_audit_log integer,
        parentid_jahia_audit_log integer,
        operation_jahia_audit_log varchar(50),
        site_jahia_audit_log varchar(50),
        content_jahia_audit_log varchar(250),
        parent_id_jahia_audit_log integer,
        eventType varchar(255),
        eventInformation blob(255),
        primary key (id_jahia_audit_log)
    );

    create table jahia_bigtext_data (
        id_bigtext_data varchar(255) not null,
        raw_value clob(16777216),
        primary key (id_bigtext_data)
    );

    create table jahia_category (
        id_category integer not null,
        key_category varchar(250) not null unique,
        aclid_category integer,
        primary key (id_category)
    );

    create table jahia_category_prop (
        id_category integer not null,
        name_category_prop varchar(250) not null,
        value_category_prop varchar(250) not null,
        primary key (id_category, name_category_prop)
    );

    create table jahia_ctn_def (
        id_jahia_ctn_def integer not null,
        jahiaid_jahia_ctn_def integer,
        name_jahia_ctn_def varchar(250),
        pctnname_jahia_ctndef_def varchar(250),
        ctntype_jahia_ctn_def varchar(150),
        primary key (id_jahia_ctn_def)
    );

    create table jahia_ctn_def_properties (
        id_jahia_ctn_def_properties integer not null,
        ctndefid_jahia_ctn_def_prop integer not null,
        pagedefid_jahia_ctn_def_prop integer,
        primary key (id_jahia_ctn_def_properties)
    );

    create table jahia_ctn_entries (
        id_jahia_ctn_entries integer not null,
        version_id integer not null,
        workflow_state integer not null,
        ctndefid_jahia_ctn_entries integer,
        rights_jahia_ctn_entries integer,
        listid_jahia_ctn_entries integer,
        pageid_jahia_ctn_entries integer,
        rank_jahia_ctn_entries integer,
        jahiaid_jahia_ctn_entries integer,
        primary key (id_jahia_ctn_entries, version_id, workflow_state)
    );

    create table jahia_ctn_lists (
        id_jahia_ctn_lists integer not null,
        version_id integer not null,
        workflow_state integer not null,
        pageid_jahia_ctn_lists integer,
        parententryid_jahia_ctn_lists integer,
        ctndefid_jahia_ctn_lists integer,
        rights_jahia_ctn_lists integer,
        primary key (id_jahia_ctn_lists, version_id, workflow_state)
    );

    create table jahia_ctn_struct (
        ctnsubdefid_jahia_ctn_struct integer not null,
        objtype_jahia_ctn_struct integer not null,
        objdefid_jahia_ctn_struct integer not null,
        rank_jahia_ctn_struct integer,
        primary key (ctnsubdefid_jahia_ctn_struct, objtype_jahia_ctn_struct, objdefid_jahia_ctn_struct)
    );

    create table jahia_ctndef_prop (
        id_jahia_ctn_def integer not null,
        name_jahia_ctndef_prop varchar(255) not null,
        value_jahia_ctndef_prop varchar(255),
        primary key (id_jahia_ctn_def, name_jahia_ctndef_prop)
    );

    create table jahia_ctnentries_prop (
        ctnid_ctnentries_prop integer not null,
        name_ctnentries_prop varchar(255) not null,
        jahiaid_ctnentries_prop integer,
        value_ctnentries_prop varchar(255),
        primary key (ctnid_ctnentries_prop, name_ctnentries_prop)
    );

    create table jahia_ctnlists_prop (
        ctnlistid_ctnlists_prop integer not null,
        name_ctnlists_prop varchar(255) not null,
        jahiaid_ctnlists_prop integer,
        value_ctnlists_prop varchar(255),
        primary key (ctnlistid_ctnlists_prop, name_ctnlists_prop)
    );

    create table jahia_db_test (
        testfield varchar(255) not null,
        primary key (testfield)
    );

    create table jahia_fieldreference (
        fieldId integer not null,
        language varchar(10) not null,
        workflow integer not null,
        target varchar(255) not null,
        siteId integer,
        primary key (fieldId, language, workflow, target)
    );

    create table jahia_fields_data (
        id_jahia_fields_data integer not null,
        version_id integer not null,
        workflow_state integer not null,
        language_code varchar(10) not null,
        connecttype_jahia_fields_data integer,
        ctnid_jahia_fields_data integer,
        fielddefid_jahia_fields_data integer,
        id_jahia_obj integer,
        type_jahia_obj varchar(22),
        rights_jahia_fields_data integer,
        pageid_jahia_fields_data integer,
        jahiaid_jahia_fields_data integer,
        type_jahia_fields_data integer,
        value_jahia_fields_data varchar(250),
        primary key (id_jahia_fields_data, version_id, workflow_state, language_code)
    );

    create table jahia_fields_def (
        id_jahia_fields_def integer not null,
        ismdata_jahia_fields_def integer,
        jahiaid_jahia_fields_def integer,
        ctnname_jahia_fields_def varchar(250),
        name_jahia_fields_def varchar(250),
        primary key (id_jahia_fields_def)
    );

    create table jahia_fields_def_extprop (
        id_jahia_fields_def integer not null,
        prop_name varchar(200) not null,
        prop_value varchar(255),
        primary key (id_jahia_fields_def, prop_name)
    );

    create table jahia_fields_prop (
        fieldid_jahia_fields_prop integer not null,
        propertyname_jahia_fields_prop varchar(250) not null,
        propvalue_jahia_fields_prop varchar(50),
        primary key (fieldid_jahia_fields_prop, propertyname_jahia_fields_prop)
    );

    create table jahia_grp_access (
        id_jahia_member varchar(150) not null,
        id_jahia_grps varchar(150) not null,
        membertype_grp_access integer not null,
        primary key (id_jahia_member, id_jahia_grps, membertype_grp_access)
    );

    create table jahia_grp_prop (
        id_jahia_grp integer not null,
        name_jahia_grp_prop varchar(50) not null,
        provider_jahia_grp_prop varchar(50) not null,
        grpkey_jahia_grp_prop varchar(200) not null,
        value_jahia_grp_prop varchar(255),
        primary key (id_jahia_grp, name_jahia_grp_prop, provider_jahia_grp_prop, grpkey_jahia_grp_prop)
    );

    create table jahia_grps (
        id_jahia_grps integer not null,
        name_jahia_grps varchar(195),
        key_jahia_grps varchar(200) unique,
        siteid_jahia_grps integer,
        hidden_jahia_grps smallint,
        primary key (id_jahia_grps)
    );

    create table jahia_indexingjobs (
        id_indexingjob varchar(50) not null,
        classname_indexingjob varchar(255) not null,
        date_indexingjob bigint,
        indeximmdty_indexingjob smallint,
        ruleId_indexingjob integer,
        fromtime1_indexingjob integer,
        totime1_indexingjob integer,
        fromtime2_indexingjob integer,
        totime2_indexingjob integer,
        fromtime3_indexingjob integer,
        totime3_indexingjob integer,
        enableserver_indexingjob varchar(100),
        ctnlistid_indexingjob integer,
        ctnid_indexingjob integer,
        siteid_indexingjob integer,
        keyname_indexingjob varchar(255),
        keyvalue_indexingjob varchar(255),
        pageid_indexingjob integer,
        fieldid_indexingjob integer,
        primary key (id_indexingjob)
    );

    create table jahia_indexingjobsserver (
        serverid varchar(200) not null,
        indexingjobid varchar(50) not null,
        indexing_date bigint,
        primary key (serverid, indexingjobid)
    );

    create table jahia_installedpatch (
        install_number integer not null,
        name varchar(100),
        build integer,
        result_code integer,
        install_date timestamp,
        primary key (install_number)
    );

    create table jahia_languages_states (
        objectkey varchar(40) not null,
        language_code varchar(10) not null,
        workflow_state integer,
        siteid integer,
        primary key (objectkey, language_code)
    );

    create table jahia_link (
        id integer not null,
        left_oid varchar(100),
        right_oid varchar(100),
        type varchar(100),
        primary key (id)
    );

    create table jahia_link_metadata (
        link_id integer not null,
        link_position varchar(20) not null,
        property_name varchar(255) not null,
        property_value varchar(255),
        primary key (link_id, link_position, property_name)
    );

    create table jahia_locks_non_excl (
        name_locks varchar(50) not null,
        targetid_locks integer not null,
        action_locks varchar(50) not null,
        context_locks varchar(80) not null,
        owner_locks varchar(50),
        timeout_locks integer,
        expirationDate_locks bigint,
        serverid_locks varchar(30),
        stolen_locks varchar(10),
        primary key (name_locks, targetid_locks, action_locks, context_locks)
    );

    create table jahia_nstep_workflow (
        id bigint not null,
        name varchar(255) not null,
        primary key (id)
    );

    create table jahia_nstep_workflowhistory (
        id bigint not null,
        action varchar(255) not null,
        author varchar(255) not null,
        message clob(1048576),
        actionDate timestamp not null,
        languageCode varchar(255) not null,
        objectKey varchar(255) not null,
        process varchar(255) not null,
        username varchar(255) not null,
        primary key (id)
    );

    create table jahia_nstep_workflowinstance (
        id bigint not null,
        authorEmail varchar(255),
        languageCode varchar(255) not null,
        objectKey varchar(255) not null,
        step bigint,
        user_id bigint,
        workflow bigint,
        startDate timestamp,
        primary key (id)
    );

    create table jahia_nstep_workflowstep (
        id bigint not null,
        name varchar(255) not null,
        workflow_id bigint,
        step_index integer,
        primary key (id)
    );

    create table jahia_nstep_workflowuser (
        id bigint not null,
        login varchar(255) not null,
        primary key (id)
    );

    create table jahia_obj (
        id_jahia_obj integer not null,
        type_jahia_obj varchar(22) not null,
        jahiaid_jahia_obj integer,
        timebpstate_jahia_obj integer,
        validfrom_jahia_obj bigint,
        validto_jahia_obj bigint,
        retrule_jahia_obj integer,
        primary key (id_jahia_obj, type_jahia_obj)
    );

    create table jahia_pages_data (
        id_jahia_pages_data integer not null,
        version_id integer not null,
        workflow_state integer not null,
        language_code varchar(10) not null,
        rights_jahia_pages_data integer,
        pagelinkid_jahia_pages_data integer,
        pagetype_jahia_pages_data integer,
        pagedefid_jahia_pages_data integer,
        parentid_jahia_pages_data integer,
        remoteurl_jahia_pages_data varchar(250),
        jahiaid_jahia_pages_data integer,
        title_jahia_pages_data varchar(250),
        primary key (id_jahia_pages_data, version_id, workflow_state, language_code)
    );

    create table jahia_pages_def (
        id_jahia_pages_def integer not null,
        jahiaid_jahia_pages_def integer,
        name_jahia_pages_def varchar(250),
        sourcepath_jahia_pages_def varchar(250),
        visible_jahia_pages_def smallint,
        browsable_jahia_pages_def integer,
        warning_msg_jahia_pages_def varchar(250),
        img_jahia_pages_def varchar(150),
        pagetype_jahia_pages_def varchar(150),
        primary key (id_jahia_pages_def)
    );

    create table jahia_pages_def_prop (
        id_jahia_pages_def_prop integer not null,
        jahiaid_pages_def_prop integer not null,
        name_pages_def_prop varchar(100) not null,
        value_pages_def_prop varchar(200),
        primary key (id_jahia_pages_def_prop, jahiaid_pages_def_prop, name_pages_def_prop)
    );

    create table jahia_pages_prop (
        page_id integer not null,
        prop_name varchar(150) not null,
        language_code varchar(100) not null,
        prop_value varchar(255),
        primary key (page_id, prop_name, language_code)
    );

    create table jahia_pages_users_prop (
        page_id integer not null,
        principal_key varchar(70) not null,
        principal_type varchar(40) not null,
        prop_type varchar(40) not null,
        prop_name varchar(150) not null,
        prop_value varchar(255),
        primary key (page_id, principal_key, principal_type, prop_type, prop_name)
    );

    create table jahia_pwd_policies (
        jahia_pwd_policy_id integer not null,
        name varchar(255) not null,
        primary key (jahia_pwd_policy_id)
    );

    create table jahia_pwd_policy_rule_params (
        jahia_pwd_policy_rule_param_id integer not null,
        name varchar(50) not null,
        position_index integer not null,
        jahia_pwd_policy_rule_id integer not null,
        type char(1) not null,
        value varchar(255),
        primary key (jahia_pwd_policy_rule_param_id)
    );

    create table jahia_pwd_policy_rules (
        jahia_pwd_policy_rule_id integer not null,
        action char(1) not null,
        rule_condition clob(1048576) not null,
        evaluator char(1) not null,
        name varchar(255) not null,
        jahia_pwd_policy_id integer not null,
        position_index integer not null,
        active smallint not null,
        last_rule smallint not null,
        periodical smallint not null,
        primary key (jahia_pwd_policy_rule_id)
    );

    create table jahia_reference (
        page_id integer not null,
        ref_id integer not null,
        ref_type integer not null,
        primary key (page_id, ref_id, ref_type)
    );

    create table jahia_resources (
        name_resource varchar(200) not null,
        languagecode_resource varchar(10) not null,
        value_resource varchar(255),
        primary key (name_resource, languagecode_resource)
    );

    create table jahia_retrule (
        id_jahia_retrule integer not null,
        id_jahia_retruledef integer,
        inherited_retrule smallint,
        enabled_retrule smallint,
        title_retrule varchar(255),
        comment_retrule varchar(255),
        shared_retrule smallint,
        settings_retrule clob(16777216),
        primary key (id_jahia_retrule)
    );

    create table jahia_retrule_range (
        id_retrule_range integer not null,
        validfrom_retrule_range bigint,
        validto_retrule_range bigint,
        notiffromd_retrule_range smallint,
        notiftod_retrule_range smallint,
        primary key (id_retrule_range)
    );

    create table jahia_retruledef (
        id_jahia_retruledef integer not null,
        name_retruledef varchar(255),
        title_retruledef varchar(255),
        ruleclass_retruledef varchar(255),
        rulehelperclass_retruledef varchar(255),
        dateformat_retruledef varchar(255),
        primary key (id_jahia_retruledef)
    );

    create table jahia_savedsearch (
        id_jahia_savedsearch integer not null,
        title_jahia_savedsearch varchar(255),
        descr_jahia_savedsearch varchar(250),
        search_jahia_savedsearch clob(16777216) not null,
        creationdate_jahia_savedsearch bigint,
        owner_jahia_savedsearch varchar(255),
        class_jahia_savedsearch varchar(255),
        jahiaid_jahia_savedsearch integer,
        rights_jahia_search integer,
        primary key (id_jahia_savedsearch)
    );

    create table jahia_savedsearchview (
        smode_savedsearchview integer not null,
        ctnid_savedsearchview varchar(100) not null,
        searchid_savedsearchview integer not null,
        userkey_savedsearchview varchar(200) not null,
        setting_savedsearchview clob(16777216) not null,
        name_savedsearchview varchar(100) not null,
        primary key (smode_savedsearchview, ctnid_savedsearchview, searchid_savedsearchview, userkey_savedsearchview)
    );

    create table jahia_serverprops (
        id_serverprops varchar(50) not null,
        propname_serverprops varchar(200) not null,
        propvalue_serverprops varchar(250) not null,
        primary key (id_serverprops, propname_serverprops)
    );

    create table jahia_site_lang_list (
        id integer not null,
        site_id integer,
        code varchar(255),
        rank integer,
        activated smallint,
        mandatory smallint,
        primary key (id)
    );

    create table jahia_site_lang_maps (
        id integer not null,
        site_id integer,
        from_lang_code varchar(255),
        to_lang_code varchar(255),
        primary key (id)
    );

    create table jahia_site_prop (
        id_jahia_site integer not null,
        name_jahia_site_prop varchar(255) not null,
        value_jahia_site_prop varchar(255),
        primary key (id_jahia_site, name_jahia_site_prop)
    );

    create table jahia_sites (
        id_jahia_sites integer not null,
        title_jahia_sites varchar(100),
        servername_jahia_sites varchar(200),
        key_jahia_sites varchar(50),
        active_jahia_sites integer,
        defaultpageid_jahia_sites integer,
        defaulttemplateid_jahia_sites integer,
        tpl_deploymode_jahia_sites integer,
        webapps_deploymode_jahia_sites integer,
        rights_jahia_sites integer,
        descr_jahia_sites varchar(250),
        default_site_jahia_sites smallint,
        primary key (id_jahia_sites)
    );

    create table jahia_sites_grps (
        grpname_sites_grps varchar(50) not null,
        siteid_sites_grps integer not null,
        grpid_sites_grps varchar(200),
        primary key (grpname_sites_grps, siteid_sites_grps)
    );

    create table jahia_sites_users (
        username_sites_users varchar(50) not null,
        siteid_sites_users integer not null,
        userid_sites_users varchar(50),
        primary key (username_sites_users, siteid_sites_users)
    );

    create table jahia_subscriptions (
        id_jahia_subscriptions integer not null,
        object_key varchar(40),
        include_children smallint not null,
        event_type varchar(50) not null,
        channel char(1) not null,
        notification_type char(1) not null,
        username varchar(255) not null,
        user_registered smallint not null,
        site_id integer not null,
        enabled smallint not null,
        suspended smallint not null,
        confirmation_key varchar(32),
        confirmation_request_timestamp bigint,
        properties clob(1048576),
        primary key (id_jahia_subscriptions)
    );

    create table jahia_url_mapping(
        mapping varchar(255) NOT NULL,
        siteid integer NOT NULL,
        pageid integer NOT NULL,
        language varchar(10),
        default_mapping char(1),
        active char(1),
        workflow_state integer,
        publication_date bigint,
        creation_date bigint,
        last_modification_date bigint,
        primary key (mapping,siteid)
    );

    create table jahia_user_prop (
        id_jahia_users integer not null,
        name_jahia_user_prop varchar(150) not null,
        provider_jahia_user_prop varchar(50) not null,
        userkey_jahia_user_prop varchar(50) not null,
        value_jahia_user_prop varchar(255),
        primary key (id_jahia_users, name_jahia_user_prop, provider_jahia_user_prop, userkey_jahia_user_prop)
    );

    create table jahia_users (
        id_jahia_users integer not null,
        name_jahia_users varchar(255),
        password_jahia_users varchar(255),
        key_jahia_users varchar(50) not null unique,
        primary key (id_jahia_users)
    );

    create table jahia_version (
        install_number integer not null,
        build integer,
        release_number varchar(20),
        install_date timestamp,
        primary key (install_number)
    );

    create table jahia_workflow (
        OBJECTKEY varchar(255) not null,
        MODEVALUE integer,
        EXTERNALNAME varchar(255),
        EXTERNALPROCESS varchar(255),
        MAINOBJECTKEY varchar(255),
        primary key (OBJECTKEY)
    );

    alter table jahia_acl_entries 
        add constraint FKDBE729858C498B01 
        foreign key (id_jahia_acl) 
        references jahia_acl;

    alter table jahia_acl_names 
        add constraint FK3C5F357D48212BE1 
        foreign key (acl_id) 
        references jahia_acl;

    alter table jahia_app_def 
        add constraint FKE6C84991455C901A 
        foreign key (rights_jahia_app_def) 
        references jahia_acl;

    alter table jahia_appdef_prop 
        add constraint FK8D4A0C287FCEE0FB 
        foreign key (appdefid_appdef_prop) 
        references jahia_app_def;

    alter table jahia_apps_share 
        add constraint FK7FE512E81F78D07 
        foreign key (site) 
        references jahia_sites;

    alter table jahia_apps_share 
        add constraint FK7FE512E83D3F9BB0 
        foreign key (definition) 
        references jahia_app_def;

    alter table jahia_audit_log 
        add constraint FKF4669B0AC75E28FD 
        foreign key (parent_id_jahia_audit_log) 
        references jahia_audit_log;

    alter table jahia_category 
        add constraint FKAFAF3D94348193C9 
        foreign key (aclid_category) 
        references jahia_acl;

    alter table jahia_ctn_def_properties 
        add constraint FKA2522525B1CEB792 
        foreign key (ctndefid_jahia_ctn_def_prop) 
        references jahia_ctn_def;

    alter table jahia_ctn_entries 
        add constraint FK6719F01827313F9D 
        foreign key (ctndefid_jahia_ctn_entries) 
        references jahia_ctn_def;

    alter table jahia_ctn_lists 
        add constraint FKB490111D2A3FB9A2 
        foreign key (ctndefid_jahia_ctn_lists) 
        references jahia_ctn_def;

    alter table jahia_ctn_struct 
        add constraint FKE9FE8F4D62299E65 
        foreign key (ctnsubdefid_jahia_ctn_struct) 
        references jahia_ctn_def_properties;

    alter table jahia_ctndef_prop 
        add constraint FK5FB05824447AD44A 
        foreign key (id_jahia_ctn_def) 
        references jahia_ctn_def;

    alter table jahia_fields_data 
        add constraint FK891B251A291A9BF4 
        foreign key (fielddefid_jahia_fields_data) 
        references jahia_fields_def;

    alter table jahia_fields_def_extprop 
        add constraint FKC2B7575A477EEAEC 
        foreign key (id_jahia_fields_def) 
        references jahia_fields_def;

    alter table jahia_grps 
        add constraint FKE530C7C492027B41 
        foreign key (siteid_jahia_grps) 
        references jahia_sites;

    alter table jahia_nstep_workflowinstance 
        add constraint FKDA6D7CCF801AE453 
        foreign key (user_id) 
        references jahia_nstep_workflowuser;

    alter table jahia_nstep_workflowinstance 
        add constraint FKDA6D7CCFA3F4D577 
        foreign key (workflow) 
        references jahia_nstep_workflow;

    alter table jahia_nstep_workflowinstance 
        add constraint FKDA6D7CCFED90E370 
        foreign key (step) 
        references jahia_nstep_workflowstep;

    alter table jahia_nstep_workflowstep 
        add constraint FK6A6E1C067F20B53 
        foreign key (workflow_id) 
        references jahia_nstep_workflow;

    alter table jahia_obj 
        add constraint FKF6E0A6A143AACCE0 
        foreign key (retrule_jahia_obj) 
        references jahia_retrule;

    alter table jahia_pages_data 
        add constraint FKB5B3A65BFC25DDC3 
        foreign key (pagedefid_jahia_pages_data) 
        references jahia_pages_def;

    alter table jahia_pages_def 
        add constraint FK1EA2B334B5FF0C79 
        foreign key (jahiaid_jahia_pages_def) 
        references jahia_sites;

    alter table jahia_pages_def_prop 
        add constraint FK8840898E47E25CC 
        foreign key (id_jahia_pages_def_prop) 
        references jahia_pages_def;

    alter table jahia_pwd_policy_rule_params 
        add constraint FKBE451EF45A0DB19B 
        foreign key (jahia_pwd_policy_rule_id) 
        references jahia_pwd_policy_rules;

    alter table jahia_pwd_policy_rules 
        add constraint FK2BC650026DA1D1E6 
        foreign key (jahia_pwd_policy_id) 
        references jahia_pwd_policies;

    alter table jahia_retrule 
        add constraint FK578E2BC72D76FCE6 
        foreign key (id_jahia_retruledef) 
        references jahia_retruledef;

    alter table jahia_retrule_range 
        add constraint FK688A96C57D611258 
        foreign key (id_retrule_range) 
        references jahia_retrule;

    alter table jahia_savedsearch 
        add constraint FK782342991382CE9E 
        foreign key (jahiaid_jahia_savedsearch) 
        references jahia_sites;

    alter table jahia_savedsearch 
        add constraint FK78234299ACC6817 
        foreign key (rights_jahia_search) 
        references jahia_acl;

    alter table jahia_site_lang_list 
        add constraint FK1DDBC16D7EED26D3 
        foreign key (site_id) 
        references jahia_sites;

    alter table jahia_site_lang_maps 
        add constraint FK1DDC17667EED26D3 
        foreign key (site_id) 
        references jahia_sites;

    alter table jahia_sites_grps 
        add constraint FK7B245597F46755FE 
        foreign key (siteid_sites_grps) 
        references jahia_sites;

    alter table jahia_sites_grps 
        add constraint FK7B24559790F996AC 
        foreign key (grpid_sites_grps) 
        references jahia_grps (key_jahia_grps);

    alter table jahia_sites_users 
        add constraint FKEA2BF1BF6CF683C0 
        foreign key (userid_sites_users) 
        references jahia_users (key_jahia_users);
