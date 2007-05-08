#@(#) ldap2sql.sql

CREATE TABLE groups (
    cid INT4 UNSIGNED NOT NULL,
    id INT4 UNSIGNED NOT NULL,
    identifier VARCHAR(128) NOT NULL,
    displayName VARCHAR(128) NOT NULL,
    lastModified INT8 NOT NULL,
    gidNumber INT4 UNSIGNED NOT NULL,
    PRIMARY KEY (cid, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE INDEX groups_identifier_idx ON groups(identifier(32));

CREATE TABLE del_groups (
    cid INT4 UNSIGNED NOT NULL,
    id INT4 UNSIGNED NOT NULL,
    identifier VARCHAR(128) NOT NULL,
    displayName VARCHAR(128) NOT NULL,
    lastModified INT8 NOT NULL,
    PRIMARY KEY (cid, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE user (
    cid INT4 UNSIGNED NOT NULL,
    id INT4 UNSIGNED NOT NULL,
    imapServer VARCHAR(128),
    imapLogin VARCHAR(128),
    mail VARCHAR(256) NOT NULL,
    mailDomain VARCHAR(128),
    mailEnabled boolean NOT NULL,
    preferredLanguage VARCHAR(10) NOT NULL,
    shadowLastChange INTEGER NOT NULL,
    smtpServer VARCHAR(128),
    timeZone VARCHAR(128) NOT NULL,
    userPassword VARCHAR(128),
    contactId INT4 UNSIGNED NOT NULL,
    passwordMech VARCHAR(32) NOT NULL,
    uidNumber INT4 UNSIGNED NOT NULL,
    gidNumber INT4 UNSIGNED NOT NULL,
    homeDirectory VARCHAR(128) NOT NULL,
    loginShell VARCHAR(128) NOT NULL,
    PRIMARY KEY (cid, id),
    INDEX (mail)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE del_user (
    cid INT4 UNSIGNED NOT NULL,
    id INT4 UNSIGNED NOT NULL,
    imapServer VARCHAR(128),
    imapLogin VARCHAR(128),
    mail VARCHAR(256) NOT NULL,
    mailDomain VARCHAR(128),
    mailEnabled boolean NOT NULL,
    preferredLanguage VARCHAR(10) NOT NULL,
    shadowLastChange INTEGER NOT NULL,
    smtpServer VARCHAR(128),
    timeZone VARCHAR(128) NOT NULL,
    contactId INT4 UNSIGNED NOT NULL,
    lastModified INT8 NOT NULL,
    userPassword VARCHAR(128),
    PRIMARY KEY (cid, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE groups_member (
    cid INT4 UNSIGNED NOT NULL,
    id INT4 UNSIGNED NOT NULL,
    member INT4 UNSIGNED NOT NULL,
    PRIMARY KEY (cid, id, member),
    FOREIGN KEY (cid, id) REFERENCES groups(cid, id),
    FOREIGN KEY (cid, member) REFERENCES user(cid, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE INDEX groups_member_cid_id_idx ON groups_member(cid, id);

CREATE TABLE login2user (
    cid INT4 UNSIGNED NOT NULL,
    id INT4 UNSIGNED NOT NULL,
    uid VARCHAR(128) NOT NULL,
    PRIMARY KEY (cid, uid),
    FOREIGN KEY (cid, id) REFERENCES user(cid, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE user_attribute (
    cid INT4 UNSIGNED NOT NULL,
    id INT4 UNSIGNED NOT NULL,
    name VARCHAR(128) NOT NULL,
    value VARCHAR(128) NOT NULL,
    INDEX (cid,name,value),
    FOREIGN KEY (cid, id) REFERENCES user(cid, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE resource (
    cid INT4 UNSIGNED NOT NULL,
    id INT4 UNSIGNED NOT NULL,
    identifier VARCHAR(128) NOT NULL,
    displayName VARCHAR(128) NOT NULL,
    mail VARCHAR(256),
    available boolean NOT NULL,
    description VARCHAR(255),
    lastModified INT8 NOT NULL,
    PRIMARY KEY (cid, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE del_resource (
    cid INT4 UNSIGNED NOT NULL,
    id INT4 UNSIGNED NOT NULL,
    identifier VARCHAR(128) NOT NULL,
    displayName VARCHAR(128) NOT NULL,
    mail VARCHAR(256),
    available boolean NOT NULL,
    description VARCHAR(255),
    lastModified INT8 NOT NULL,
    PRIMARY KEY (cid, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE INDEX resource_identifier_idx ON resource(identifier(32));
