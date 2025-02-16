package com.media.MediaStreaming.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.media.MediaStreaming.Models.Media;
import com.media.MediaStreaming.Models.VideoDetails;

@Repository
public interface VideoDetailsRepository extends JpaRepository<VideoDetails, Long> {

    @Query("SELECT vd FROM VideoDetails vd " +
            "JOIN vd.media m " +
            "WHERE m.externalUserId = :externalUserId " +
            "AND vd.uniqueVideoId = :uniqueVideoId")
    Optional<VideoDetails> findByMediaExternalUserIdAndUniqueVideoId(
            @Param("externalUserId") String externalUserId,
            @Param("uniqueVideoId") String uniqueVideoId);
}
