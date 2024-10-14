package org.demo.util;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.demo.entity.ChangelogLockEntity;
import org.demo.model.CQLChangeLog;
import org.demo.model.ChangeSet;
import org.demo.model.Include;
import org.demo.repository.ChangelogLockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;

public class DBValidator implements Closeable {


    private static final Logger log = LoggerFactory.getLogger(DBValidator.class);
    private final CqlSession cqlSession;
    private final String identifier;
    private final ChangelogLockRepository changelogLockRepository;
    private final AtomicBoolean updated = new AtomicBoolean(false);
    private final Pair<CQLChangeLog, CQLChangeLog> changeLogs;
    private final String md5Key;

    public DBValidator(CqlSession cqlSession, String identifier) {
        this.cqlSession = cqlSession;
        this.identifier = identifier;
        this.changelogLockRepository = new ChangelogLockRepository(this.cqlSession);
        this.changeLogs = this.getPrimaryChangeLog();
        List<ChangeSet> orderedChangeSets = changeLogs.getSecond().getChangeSets().stream().sorted(Comparator.comparing(ChangeSet::getOrder)).toList();
        final StringJoiner keyAsString = new StringJoiner("#");
        for (ChangeSet changeSet : orderedChangeSets) {
            changeSet.validate();
            keyAsString.add(changeSet.getMd5Sum());
        }
        this.md5Key = HashGenerator.generateHash(keyAsString.toString(), HashGenerator.ALGType.MD5);
        log.info("Major version Key (MD5): {}", md5Key);
    }


    public void init() {
        if (!this.changelogLockRepository.tableExists()) {
            changeLogs.getFirst().getChangeSets().stream().sorted(Comparator.comparing(ChangeSet::getOrder)).forEach(this::execute);
            log.info("Changelog Tables created.");
        } else {
            log.info("Changelog Tables already exists.");
        }
        final boolean isAcquired = this.changelogLockRepository.acquireLock(this.md5Key, changeLogs.getFirst().getMajorVersion(), this.identifier);
        this.updated.set(isAcquired);
        if (isAcquired) {
            log.info("Lock acquired By {}. Proceeding to update.", this.identifier);
        } else {
            int count = 0;
            boolean isDone = false;
            while (!isDone) {
                try {
                    ChangelogLockEntity entity = this.changelogLockRepository.getByLockId(this.md5Key, changeLogs.getFirst().getMajorVersion());
                    if (entity.getLocked()) {
                        Thread.sleep(1_000);
                        log.info("Waiting for lock... Lock is being acquired by another instance: {}", entity.getLockedBy());
                    }
                    isDone = !entity.getLocked();
                    count++;
                    if (count > 20) {
                        log.warn("It seems like the acquirer crashed unexpectedly.");
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            log.info("The Update is already done by another instance. Nothing to update.");
        }
    }

    public void validate() {
        this.init();
        if (this.updated.get()) {
            log.info("Start applying changes...");
            this.saveChangeLog(changeLogs.getSecond());
        }
    }


    private void saveChangeLog(CQLChangeLog changeLog) {
        changeLog.getChangeSets().stream().sorted(Comparator.comparing(ChangeSet::getOrder)).forEach(this::execute);
    }

    private void execute(ChangeSet changeSet) {
        SimpleStatement simpleStatement = SimpleStatement.newInstance(changeSet.getStatement()).setConsistencyLevel(changeSet.getConsistencyLevel());
        log.info("Updating Change log. [ChangeLogId: {}, Author : {}, Order : {}, RowKey : {}]", changeSet.getId(), changeSet.getAuthor(), changeSet.getOrder(), changeSet.getMd5Sum());
        cqlSession.execute(simpleStatement);
    }

    private File buildChangeLogPath(String[] paths) {
        return Paths.get("src/main/resources/db", paths).toFile();
    }

    private Pair<CQLChangeLog, CQLChangeLog> getPrimaryChangeLog() {
        try {
            final File file = this.buildChangeLogPath(new String[]{"changeset-config.xml"});
            if (file.exists()) {
                log.info("file exists: {}", file.getAbsolutePath());
            }
            final JAXBContext jaxbContext = JAXBContext.newInstance(CQLChangeLog.class);
            final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            final CQLChangeLog changeLog = (CQLChangeLog) jaxbUnmarshaller.unmarshal(file);
            List<Include> includeList = changeLog.getIncludeList();

            CQLChangeLog primaryChangeLog = includeList.stream()
                    .filter(Include::isPrimary)
                    .findFirst()
                    .map(include -> {
                        try {
                            File file1 = this.buildChangeLogPath(new String[]{include.getPath()});
                            if (file1.exists()) {
                                log.info("file exists: {}", file1.getAbsolutePath());
                            }
                            return (CQLChangeLog) jaxbUnmarshaller.unmarshal(file1);
                        } catch (JAXBException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .orElseThrow(() -> new RuntimeException("primary change log not found"));
            primaryChangeLog.setMajorVersion(changeLog.getMajorVersion());
            CQLChangeLog secondaryChangeLog = includeList.stream().filter(include -> !include.isPrimary()).findFirst().map(include -> {
                try {
                    File file1 = this.buildChangeLogPath(new String[]{include.getPath()});
                    if (file1.exists()) {
                        log.info("file exists: {}", file1.getAbsolutePath());
                    }
                    return (CQLChangeLog) jaxbUnmarshaller.unmarshal(file1);
                } catch (JAXBException e) {
                    throw new RuntimeException(e);
                }
            }).orElseThrow(() -> new RuntimeException("primary change log not found"));
            return Pair.of(primaryChangeLog, secondaryChangeLog);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void close() {
        if (this.updated.get()) {
            if (this.changelogLockRepository.releaseLock(this.md5Key, changeLogs.getFirst().getMajorVersion(), this.identifier)) {
                log.info("lock released successfully [{}]", this.md5Key);
            } else {
                log.warn("lock released failed [{}]", this.md5Key);
            }
        }
    }
}
