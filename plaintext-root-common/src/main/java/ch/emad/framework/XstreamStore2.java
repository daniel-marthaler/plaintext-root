package ch.emad.framework;

import com.thoughtworks.xstream.XStream;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Xstream accessor
 *
 * @author Author: info@emad.ch
 * @since 0.0.1
 */
@Controller
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class XstreamStore2<T extends Xstream2Storable> {

    private XStream xstream = new XStream();

    @Autowired
    private TextRepository2 repo;

    @Autowired
    private EmadSecWrapper sec;

    @PostConstruct
    private void init(){
        xstream.allowTypesByWildcard(new String[] {
                "ch.emad.**"
        });
    }

    public T readByKeyAndMandant(String key, String mandant) {
        Text2 in = repo.findByKeyAndMandat(key, mandant);
        if (in == null) {
            return null;
        }

        T temp = (T) xstream.fromXML(in.getValue());
        temp.setLastModifiedBy(in.getLastModifiedBy());
        temp.setLastModifiedDate(in.getLastModifiedDate());

        return temp;
    }

    public List<T> readByIndex(String index, String mandat) {
        List<T> ret = new ArrayList<>();
        List<Text2> in = repo.findByIndexAndMandat(index, mandat);
        if (in == null) {
            return new ArrayList<>();
        }
        for (Text2 tx : in) {
            T temp = (T) xstream.fromXML(tx.getValue());
            temp.setLastModifiedBy(tx.getLastModifiedBy());
            temp.setLastModifiedDate(tx.getLastModifiedDate());
            ret.add(temp);
        }
        return ret;
    }

    public List<T> readByType(Class<T> type) {
        List<T> ret = new ArrayList<>();
        List<Text2> in = repo.findByType(type.getSimpleName());
        if (in == null) {
            return new ArrayList<>();
        }
        for (Text2 tx : in) {
            T temp = (T) xstream.fromXML(tx.getValue());
            temp.setLastModifiedBy(tx.getLastModifiedBy());
            temp.setLastModifiedDate(tx.getLastModifiedDate());
            ret.add(temp);
        }
        return ret;
    }

    public List<T> readByTypeAndMandat(Class<T> type, String mandat) {
        List<T> ret = new ArrayList<>();
        List<Text2> in = repo.findByTypeAndMandat(type.getSimpleName(), mandat);
        if (in == null) {
            return new ArrayList<>();
        }
        for (Text2 tx : in) {
            T temp = (T) xstream.fromXML(tx.getValue());
            temp.setLastModifiedBy(tx.getLastModifiedBy());
            temp.setLastModifiedDate(tx.getLastModifiedDate());
            ret.add(temp);
        }
        return ret;
    }

    public T save(T obj, String mandat) {
        Text2 in = repo.findByKeyAndMandat(obj.getKey(), mandat);

        if (in == null) {
            in = new Text2();
            in.setKey(obj.getKey());
            in.setValue(xstream.toXML(obj));
            in = repo.save(in);
            obj.setCreatedBy(in.getCreatedBy());
        }

        in.setKey(obj.getKey());
        in.setType(obj.getClass().getSimpleName());
        in.setIndex(obj.getIndex());
        in.setIndex1(obj.getIndex1());
        in.setIndex2(obj.getIndex2());
        in.setIndex3(obj.getIndex3());
        in.setIndex4(obj.getIndex4());

        obj.setMandat(mandat);
        in.setMandat(mandat);

        obj.setLastModifiedDate(new Date());
        in.setValue(xstream.toXML(obj));
        repo.save(in);
        return readByKeyAndMandant(obj.getKey(), mandat);
    }

    public void delete(T obj) {
        Text2 in = repo.findByKey(obj.getKey());
        repo.deleteById(in.getId());
    }

}
