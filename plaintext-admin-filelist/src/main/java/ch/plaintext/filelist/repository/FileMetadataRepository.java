package ch.plaintext.filelist.repository;

import ch.plaintext.filelist.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    List<FileMetadata> findByMandat(String mandat);

    List<FileMetadata> findByMandatOrderByCreatedDateDesc(String mandat);

    List<FileMetadata> findByMandatAndCategory(String mandat, String category);

    List<FileMetadata> findByMandatAndStorageBackend(String mandat, String storageBackend);

    List<FileMetadata> findByUploadedBy(String username);

    @Query("SELECT DISTINCT f.category FROM FileMetadata f WHERE f.mandat = :mandat")
    List<String> findDistinctCategoriesByMandat(String mandat);

    @Query("SELECT DISTINCT f.storageBackend FROM FileMetadata f WHERE f.mandat = :mandat")
    List<String> findDistinctStorageBackendsByMandat(String mandat);

    @Query("SELECT SUM(f.fileSize) FROM FileMetadata f WHERE f.mandat = :mandat")
    Long calculateTotalStorageUsed(String mandat);

    List<FileMetadata> findByMandatAndFilenameContaining(String mandat, String filename);
}
