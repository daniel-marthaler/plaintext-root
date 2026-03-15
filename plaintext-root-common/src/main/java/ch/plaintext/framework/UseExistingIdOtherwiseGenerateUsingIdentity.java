package ch.plaintext.framework;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentityGenerator;

import java.io.Serializable;

@Slf4j
public class UseExistingIdOtherwiseGenerateUsingIdentity extends IdentityGenerator {

    private long idb = 1000000L;

    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {

        synchronized (this) {
            Serializable id = (Serializable) session.getEntityPersister(null, object).getClassMetadata().getIdentifier(object, session);

            if (id != null) {
                return id;
            } else {

                if (RepoMaster.instance == null) {
                    idb++;
                    log.error("ACHTUNG ID OHNE SPRING: " + idb + " FUER: " + object.getClass().getSimpleName());
                    return idb;

                }

                Long idN = RepoMaster.instance.getNextID(object);
                return idN;

            }
        }

    }
}