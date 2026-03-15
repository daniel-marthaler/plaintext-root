package ch.plaintext.boot.plugins.objstore;

/**
 * @author : mad
 * @since : 26.08.2024
 **/
public interface SimpleStorable<T> {

    String getUniqueId();

    void setUniqueId(String id);

}